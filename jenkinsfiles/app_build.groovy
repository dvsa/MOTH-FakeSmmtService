@Library('PipelineUtils@master')
import dvsa.aws.mot.jenkins.pipeline.common.AWSFunctions
import dvsa.aws.mot.jenkins.pipeline.common.RepoFunctions
import dvsa.aws.mot.jenkins.pipeline.common.GlobalValues

def awsFunctionsFactory  = new AWSFunctions()
def repoFunctionsFactory = new RepoFunctions()
def globalValuesFactory  = new GlobalValues()


// This should be a parameter to the pipeline
String environment            = 'dawidm'
String account                = 'dev'

// Static stuff
String project                = 'vehicle-recalls'
String jenkinsctrl_node_label = 'ctrl'
String brach                  = params.BRANCH
String build_id              = env.BUILD_NUMBER

// Pipeline specific data
String bucket_prefix = 'terraformscaffold'
String bucket        = bucket_prefix + environment

Map<String, Map<String, String>> gitlab = [
  infastructure: [
    group: 'vehicle-recalls',
    name: 'recalls-infrastructure',
    branch: branch
  ]
]

Map<String, Map<String, String>> github = [
  fake_smmt: [
    group: 'dvsa',
    name: 'vehicle-recalls-fake-smmt-service',
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

// TODO Remove this after rework.
TF_LOG_LEVEL = 'ERROR'
TF_PROJECT = 'vehicle-recalls'
BUCKET_PREFIX = 'uk.gov.dvsa.vehicle-recalls.'

def sh_output(String script) {
  return sh(
    script: script,
    returnStdout: true
  ).trim()
}

def sh_status(String script) {
  return sh(
    script: script,
    returnStatus: true
  )
}

def log_info(String info) {
  echo "[INFO] ${info}"
}

def bucket_exists(String bucket) {
  return sh_status("aws s3 ls s3://${bucket} --region ${AWS_REGION} 2>&1 | grep -q -e \'NoSuchBucket\' -e \'AccessDenied\'")
}

def verify_or_create_bucket(String bucket_prefix, String tf_component) {
  bucket = bucket_prefix + ENV

  if (bucket_exists(bucket) == 1) {
    log_info("Bucket ${bucket} found")
  } else {
    log_info("Bucket ${bucket} not found.")
    log_info("Creating Bucket")

    node('ctrl' && 'dev') {
      fetch_infrastructure_code()

      extra_args = "-var environment=${ENV} " +
        "-var bucket_prefix=${bucket_prefix}"

      tf_scaffold('plan', tf_component, extra_args)
      tf_scaffold('apply', tf_component, extra_args)
    }
  }
}

Boolean buildNPM(
  String directory,
  String buildStamp
){
  dir(directory) {
    Integer status = sh(
      script: """
        npm install
        BUILDSTAMP=${buildStamp} npm run build
      """,
      returnStatus: true
    )
    return status
  }
}

def build_and_upload_js(bucket,build_id) {


    return
    return dist_file
  }


def abort_build(String message) {
  currentBuild.result = 'ABORTED'
  error(message)
}

def copy_file_to_s3(file, bucket) {
  sh("aws s3 cp $file s3://$bucket")
}

def checkout_github_repo(String group, String repo, String branch) {
  checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: branch]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CloneOption', depth: 0, noTags: true, reference: '', shallow: true], [$class: 'RelativeTargetDirectory', relativeTargetDir: repo]], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/' + group + '/' + repo + '.git']]]
}

def checkout_github_repo_branch_or_master(group, repo, branch) {
  try {
    checkout_github_repo(group, repo, branch)
  } catch(error) {
    log_info("${error}")
    checkout_github_repo(group, repo, "master")
  }
}

def checkout_gitlab_repo(String group, String repo, String branch) {
  log_info("Checkout GITLAB repo \"${group}/${repo}\" branch \"${branch}")

  creds = '313a82d3-f2e7-4787-837e-7517f3ce84eb'
  checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: branch]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CloneOption', depth: 0, noTags: true, reference: '', shallow: true], [$class: 'RelativeTargetDirectory', relativeTargetDir: repo]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: creds, url: 'git@gitlab.motdev.org.uk:' + group + '/' + repo + '.git']]]
}

def checkout_gitlab_repo_branch_or_master(group, repo, branch) {
  try {
    checkout_gitlab_repo(group, repo, branch)
  } catch(error) {
    log_info("${error}")
    checkout_gitlab_repo(group, repo, "master")
  }
}

def tf_scaffold(action, component, extra_args, returnStdout=false) {
  log_info("Terraform ${action} on \"${component}\" with \"${extra_args}\"")

  env_type = 'FB'

  withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: env_type + '_AWS_CREDENTIALS', usernameVariable: 'aws_key_id', passwordVariable: 'aws_secret_key']]) {
    withEnv([
      "AWS_DEFAULT_REGION=${AWS_REGION}",
      "AWS_ACCESS_KEY_ID=${env.aws_key_id}",
      "AWS_SECRET_ACCESS_KEY=${env.aws_secret_key}",
      'PATH+=/opt/tfenv/bin',
      "TF_LOG=${TF_LOG_LEVEL}"
    ]) {
      return sh(
        script: """
        export TFENV_DEBUG=0
        bash -x recalls-infrastructure/bin/terraform.sh \
          --action ${action} \
          --project ${TF_PROJECT} \
          --environment ${ENV} \
          --component ${component} \
          --region ${AWS_REGION} \
          -- ${extra_args}
        """,
        returnStdout: returnStdout
      )
    }
  }
}

def tf_output(variable, tf_component) {
  //returning last line of terraform scaffold output
  return tf_scaffold("output ${variable}", tf_component, "", true).split("\n")[-1]
}

def populate_tfvars(key, value) {
  log_info("Populating tfvars \"${key}\" with \"${value}\"")

  tfvars_path = "env_${AWS_REGION}_${ENV}.tfvars"

  sh("sed -i 's|%${key}%|${value}|g' recalls-infrastructure/etc/${tfvars_path}")
}

def get_tfenv() {
  if (!fileExists('/opt/tfenv/bin/tfenv')) {
    dir('/opt') { check_out_github_repo('cartest', 'tfenv', 'master') }
  } else {
    log_info('TFENV already installed. Skipping...')
  }
}

def build_and_deploy_lambda(params) {
  String name                   = params.name
  String repo                   = params.repo
  String tf_component           = params.tf_component
  String code_branch            = params.code_branch
  String bucket_prefix          = params.bucket_prefix
  String bucket                 = bucket_prefix + params.environment
  String build_id               = params.build_id
  String jenkinsctrl_node_label = params.jenkinsctrl_node_label
  String account                = params.account
  String environment            = params.environment
  String project                = params.project
  def github                    = params.github
  def gitlab                    = params.gitlab
  def repoFunctionsFactory      = params.repoFunctionsFactory
  def awsFunctionsFactory       = params.awsFunctionsFactory
  def globalValuesFactory       = params.globalValuesFactory
  String dist_file
  tfvars = params.tfvars
  log_info("========================")
  log_info("name: ${name}")
  log_info("repo: ${repo}")
  log_info("tf_component: ${tf_component}")
  log_info("code_branch: ${code_branch}")
  log_info("bucket_prefix: ${bucket_prefix}")
  log_info("bucket: ${bucket}")
  log_info("tfvars: ${tfvars}")
  log_info("========================")

  stage('Build ' + name) {
    repoFunctionsFactory.checkoutGitRepo(
      github.fake_smmt.url,
      'master', // Change that to branch after tests
      github.fake_smmt.name, // We will agree together on the naming - probably we will use gitlab.infastructure.name
      globalValuesFactory.SSH_DEPLOY_GIT_CREDS_ID
    )
    dir(github.fake_smmt.name) {
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
      }
    }
  }

  stage('TF Plan & Apply ' + name) {
    wrap([
      $class: 'TimestamperBuildWrapper'
    ]) {
      wrap([
        $class:       'AnsiColorBuildWrapper',
        colorMapName: 'xterm'
      ]) {
        node(jenkinsctrl_node_label&&account) {
          repoFunctionsFactory.checkoutGitRepo(
            gitlab.infastructure.url,
            gitlab.infastructure.branch,
            gitlab.infastructure.name,
            globalValuesFactory.SSH_DEPLOY_GIT_CREDS_ID
          )
          String lambda_s3_key = dist_file.substring(dist_file.lastIndexOf("/")).replaceAll('/','')
          dir(gitlab.infastructure.name) {
            awsFunctionsFactory.terraformScaffold(
              project,
              environment,
              account,
              globalValuesFactory.AWS_REGION,
              "-var lambda_s3_key=${lambda_s3_key}",
              'terraform_plan',
              build_number,
              tf_component,
              bucket_prefix,
              'apply'
            )
          }
          dir(gitlab.infastructure.name+"/components/"+tf_component+"/.terraform") {
            sh("ls -la")
          }

          log_info("TERRAFORM OUTPUT: ${terraform_output}")

          return
          sh('ls -la')

          if(tfvars) {
            tfvars.each { entry ->
              populate_tfvars(entry.key, entry.value)
            }
          }
          populate_tfvars("lambda_s3_key", dist)

          tf_scaffold('plan', tf_component, "")
          tf_scaffold('apply', tf_component, "")

          return tf_output('api_gateway_url', tf_component)
        }
      }
      return
    }
  }
}

node(jenkinsctrl_node_label&&account) {
  stage('Verify S3 Bucket') {
    wrap([
      $class: 'TimestamperBuildWrapper'
    ]) {
      wrap([
        $class:       'AnsiColorBuildWrapper',
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
              'apply' // When devs agree on this change we will change plan to apply.
            )
          }
        }
      }
    }
  }

node('builder') {
    fake_smmt_url = build_and_deploy_lambda(
      name: 'Fake SMMT',
      bucket_prefix: bucket_prefix,
      repo: github.fake_smmt.name,
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
    return
    build_and_deploy_lambda(
      name: 'Vehicle Recalls',
      environment: environment,
      bucket_prefix: BUCKET_PREFIX,
      repo: 'vehicle-recalls-api',
      tf_component: 'vehicle_recalls_api',
      code_branch: BRANCH,
      tfvars: [
        "fake_smmt_url": fake_smmt_url
      ]
    )
}
