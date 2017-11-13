@Library('PipelineUtils@master')
import dvsa.aws.mot.jenkins.pipeline.common.AWSFunctions
import dvsa.aws.mot.jenkins.pipeline.common.RepoFunctions
import dvsa.aws.mot.jenkins.pipeline.common.GlobalValues

def awsFunctionsFactory = new AWSFunctions()
def repoFunctionsFactory = new RepoFunctions()
def globalValuesFactory = new GlobalValues()

// This should be a parameter to the pipeline
String environment = 'int'
String account = 'dev'

// Static stuff
String project = 'vehicle-recalls'
String jenkinsctrl_node_label = 'ctrl'
String brach = params.BRANCH
String build_id = env.BUILD_NUMBER

// Pipeline specific data
String bucket_prefix = 'terraformscaffold'

Map<String, Map<String, String>> gitlab = [
  infastructure: [
    group : 'vehicle-recalls',
    name  : 'recalls-infrastructure',
    branch: branch
  ]
]

Map<String, Map<String, String>> github = [
  fake_smmt          : [
    group : 'dvsa',
    name  : 'vehicle-recalls-fake-smmt-service',
    branch: branch
  ],
  vehicle_recalls_api: [
    group : 'dvsa',
    name  : 'vehicle-recalls-api',
    branch: branch
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
          "uk.gov.dvsa.vehicle-recalls.${environment}",
          dist_file,
          'FB_AWS_CREDENTIALS'
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
  def gitlab = params.gitlab

  log_info(gitlab.infastructure.name)

  def repoFunctionsFactory = params.repoFunctionsFactory
  def awsFunctionsFactory = params.awsFunctionsFactory
  def globalValuesFactory = params.globalValuesFactory

  stage('TF Plan & Apply ' + name) {
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

          fake_smmt_dist = fix_dist_path(fake_smmt_dist)
          vehicle_recalls_api_dist = fix_dist_path(vehicle_recalls_api_dist)

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
              'apply'
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

def populate_tfvars(key, value, environment, region) {
  log_info("Populating tfvars \"${key}\" with \"${value}\"")

  tfvars_path = "env_${region}_${environment}.tfvars"

  sh("sed -i 's|%${key}%|${value}|g' recalls-infrastructure/etc/${tfvars_path}")
}

node(jenkinsctrl_node_label && account) {
  stage('Verify S3 Bucket') {
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
            build_number,
            's3',
            bucket_prefix,
            'apply'
          )
        }
      }
    }
  }
}

node('builder') {
// Cleanup will remove build_and_deploy_lambda. The plan is to have build and deploy definition.
  fake_smmt_dist = stage_build_and_upload_js(
    name: 'Fake SMMT',
    bucket_prefix: bucket_prefix,
    repo: github.fake_smmt.url,
    tf_component: 'fake_smmt',
    code_branch: brach,
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
    code_branch: brach,
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

  log_info(gitlab.infastructure.name)

  log_info(vehicle_recalls_api_dist)

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
    globalValuesFactory: globalValuesFactory
  )
}