@Library('PipelineUtils@master')
import dvsa.aws.mot.jenkins.pipeline.common.AWSFunctions
import dvsa.aws.mot.jenkins.pipeline.common.RepoFunctions
import dvsa.aws.mot.jenkins.pipeline.common.GlobalValues

awsFunctionsFactory = new AWSFunctions()
repoFunctionsFactory = new RepoFunctions()
globalValuesFactory = new GlobalValues()

// This should be a parameter to the pipeline
environment = 'int'
account = params.AWS_ACCOUNT

// Static stuff
project = 'vehicle-recalls'
jenkinsctrl_node_label = 'ctrl'
build_branch = params.BRANCH
action = params.ACTION
build_id = env.BUILD_NUMBER

// Pipeline specific data
bucket_prefix = 'terraformscaffold'

gitlab = [
  infastructure: [
    group : 'vehicle-recalls',
    name  : 'recalls-infrastructure',
    branch: build_branch
  ]
]

github = [
  fake_smmt: [
    group : 'dvsa',
    name  : 'vehicle-recalls-fake-smmt-service',
    branch: build_branch
  ],
  vehicle_recalls_api: [
    group : 'dvsa',
    name  : 'vehicle-recalls-api',
    branch: build_branch
  ]
]

for (repo in gitlab.keySet()) {
  if (!gitlab[repo].url) {
    gitlab[repo].url = "${globalValuesFactory.GITLAB_URL}:${gitlab[repo].group}/${gitlab[repo].name}.git"
  }
}

for (repo in github.keySet()) {
  if (!github[repo].url) {
    github[repo].url = "https://github.com/${github[repo].group}/${github[repo].name}.git"
  }
}

// We will move this to library.
def log_info(String info) {
  echo "[INFO] ${info}"
}

Boolean buildNPM(
  String directory,
  String buildStamp
) {
  dir(directory) {
    Integer status = sh(
      script: """
        npm install yarn --save-dev && ./node_modules/yarn/bin/yarn
        BUILDSTAMP=${buildStamp} npm run build
      """,
      returnStatus: true
    )
    return status
  }
}


def stage_build_and_upload_js(params) {
  String name = params.name
  String repo = params.repo
  String tf_component = params.tf_component
  String code_branch = params.code_branch
  String bucket_prefix = params.bucket_prefix
  String bucket = bucket_prefix + params.environment
  String build_id = params.build_id
  String environment = params.environment
  def repoFunctionsFactory = params.repoFunctionsFactory
  def awsFunctionsFactory = params.awsFunctionsFactory
  def globalValuesFactory = params.globalValuesFactory

  String dist_file
  // Some cleanup
  log_info("========================")
  log_info("name: ${name}")
  log_info("repo: ${repo}")
  log_info("tf_component: ${tf_component}")
  log_info("code_branch: ${code_branch}")
  log_info("bucket_prefix: ${bucket_prefix}")
  log_info("bucket: ${bucket}")
  log_info("repo: ${repo}")
  String repoDir = repo.substring(repo.lastIndexOf("/")).replaceAll('/', '') // This is important dont cleanup this
  log_info("${repoDir}")
  log_info("========================")
  stage('Build ' + name) {
    repoFunctionsFactory.checkoutGitRepo(
      repo,
      'master', // Change that to branch after tests - during pipeline development i wantedo to use the latest master.
      repoDir,
      globalValuesFactory.SSH_DEPLOY_GIT_CREDS_ID
    )
    dir(repoDir) {
      buildNPM(
        'app',
        build_id
      )
      dir("app/dist") {
        dist_file = sh(script: "find . -type f -name \'*-${build_id}.zip\'", returnStdout: true).trim()
        log_info("Found: ${dist_file}")
        awsFunctionsFactory.copyToS3(
          "uk.gov.dvsa.vrec.${environment}",
          dist_file,
          'MOT_TEST_AWS_CREDENTIALS'
        )
        return dist_file
      }
    }
  }
}

def stage_tf_plan_and_apply(params) {
  String name = params.name
  String tf_component = params.tf_component
  String fake_smmt_dist = params.fake_smmt_dist
  String vehicle_recalls_api_dist = params.vehicle_recalls_api_dist

  String jenkinsctrl_node_label = params.jenkinsctrl_node_label
  String account = params.account
  String environment = params.environment
  String project = params.project
  String bucket_prefix = params.bucket_prefix
  String tf_command = params.tf_command
  def gitlab = params.gitlab

  log_info(gitlab.infastructure.name)

  def repoFunctionsFactory = params.repoFunctionsFactory
  def awsFunctionsFactory = params.awsFunctionsFactory
  def globalValuesFactory = params.globalValuesFactory

  stage('TF tf_component:' + tf_component + ', action:' + params.tf_command) {
    wrap([
      $class: 'TimestamperBuildWrapper'
    ]) {
      wrap([
        $class      : 'AnsiColorBuildWrapper',
        colorMapName: 'xterm'
      ]) {
        node(jenkinsctrl_node_label && account) {
          repoFunctionsFactory.checkoutGitRepo(
            gitlab.infastructure.url,
            gitlab.infastructure.branch,
            gitlab.infastructure.name,
            globalValuesFactory.SSH_DEPLOY_GIT_CREDS_ID
          )

          dir(gitlab.infastructure.name) {
            awsFunctionsFactory.terraformScaffold(
              project,
              environment,
              account,
              globalValuesFactory.AWS_REGION,
              "-var fake_smmt_lambda_s3_key=${fake_smmt_dist} -var vehicle_recalls_api_lambda_s3_key=${vehicle_recalls_api_dist}",
              'terraform_plan',
              build_number,
              tf_component,
              bucket_prefix,
              tf_command
            )
          }
        }
      }
    }
  }
}

def fix_dist_path(dist_path) {
  return dist_path.substring(dist_path.lastIndexOf("/")).replaceAll('/', '')
}

def s3_bucket_repo(params) {

  String tf_command = params.tf_command
  node(jenkinsctrl_node_label && account) {
    deleteDir();

    stage('S3 repo Bucket: ' + tf_command) {
      wrap([
        $class: 'TimestamperBuildWrapper'
      ]) {
        wrap([
          $class      : 'AnsiColorBuildWrapper',
          colorMapName: 'xterm'
        ]) {
          repoFunctionsFactory.checkoutGitRepo(
            gitlab.infastructure.url,
            gitlab.infastructure.branch,
            gitlab.infastructure.name,
            globalValuesFactory.SSH_DEPLOY_GIT_CREDS_ID
          )
          dir(gitlab.infastructure.name) {
            awsFunctionsFactory.terraformScaffold(
              project,
              environment,
              account,
              globalValuesFactory.AWS_REGION,
              '',    // I'm not passing any extra args - lets keep this generic
              'terraform_plan',
              build_id,
              's3',
              bucket_prefix,
              params.tf_command
            )
          }
        }
      }
    }
  }
}

if(action == 'apply') s3_bucket_repo(tf_command: action)

node('builder') {
// Cleanup will remove build_and_deploy_lambda. The plan is to have build and deploy definition.

  deleteDir()
  String fake_smmt_dist
  String vehicle_recalls_api_dist

  if(action == 'apply') {
    fake_smmt_dist = stage_build_and_upload_js(
      name: 'Fake SMMT',
      bucket_prefix: bucket_prefix,
      repo: github.fake_smmt.url,
      tf_component: 'fake_smmt',
      code_branch: build_branch,
      environment: environment,
      awsFunctionsFactory: awsFunctionsFactory,
      repoFunctionsFactory: repoFunctionsFactory,
      globalValuesFactory: globalValuesFactory,
      github: github,
      gitlab: gitlab,
      build_id: build_id,
      jenkinsctrl_node_label: jenkinsctrl_node_label,
      account: account,
      project: project
    )
    log_info(fake_smmt_dist)

    log_info(gitlab.infastructure.name)

    vehicle_recalls_api_dist = stage_build_and_upload_js(
      name: 'Vehicle Recalls API',
      bucket_prefix: bucket_prefix,
      repo: github.vehicle_recalls_api.url,
      tf_component: 'vehicle_recalls_api',
      code_branch: build_branch,
      environment: environment,
      awsFunctionsFactory: awsFunctionsFactory,
      repoFunctionsFactory: repoFunctionsFactory,
      globalValuesFactory: globalValuesFactory,
      github: github,
      gitlab: gitlab,
      build_id: build_id,
      jenkinsctrl_node_label: jenkinsctrl_node_label,
      account: account,
      project: project
    )

    fake_smmt_dist = fix_dist_path(fake_smmt_dist)
    vehicle_recalls_api_dist = fix_dist_path(vehicle_recalls_api_dist)
    log_info(vehicle_recalls_api_dist)
  }

  stage_tf_plan_and_apply(
    name: "Vehicle Recalls",
    bucket_prefix: bucket_prefix,
    tf_component: "vehicle_recalls",
    fake_smmt_dist: fake_smmt_dist,
    vehicle_recalls_api_dist: vehicle_recalls_api_dist,
    jenkinsctrl_node_label: jenkinsctrl_node_label,
    gitlab: gitlab,
    account: account,
    project: project,
    environment: environment,
    awsFunctionsFactory: awsFunctionsFactory,
    repoFunctionsFactory: repoFunctionsFactory,
    globalValuesFactory: globalValuesFactory,
    tf_command: action
  )

  if(action == 'destroy') s3_bucket_repo(tf_command: action)
}
