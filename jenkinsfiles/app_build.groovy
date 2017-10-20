def sh_output(String script) {
  return sh(
    script: script,
    returnStdout: true
  ).trim()
}

def abort_build(String message) {
  currentBuild.result = 'ABORTED'
  error(message)
}

def copy_file_to_s3(file, bucket) {
  sh("aws s3 cp $file s3://$bucket")
}

def checkout_github_repo(group, repo, branch) {
  checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: branch]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CloneOption', depth: 0, noTags: true, reference: '', shallow: true], [$class: 'RelativeTargetDirectory', relativeTargetDir: repo]], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/' + group + '/' + repo + '.git']]]
}

def checkout_gitlab_repo(group, repo, branch, creds) {
  checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: branch]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CloneOption', depth: 0, noTags: true, reference: '', shallow: true], [$class: 'RelativeTargetDirectory', relativeTargetDir: repo]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: creds, url: 'git@gitlab.motdev.org.uk:' + group + '/' + repo + '.git']]]
}

def tf_scaffold(env_type, tf_log_level, action, project, environment, component, region, extra_args) {
  withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: env_type + '_AWS_CREDENTIALS', usernameVariable: 'aws_key_id', passwordVariable: 'aws_secret_key']]) {
    withEnv([
      "AWS_DEFAULT_REGION=${aws_region}",
      "AWS_ACCESS_KEY_ID=${env.aws_key_id}",
      "AWS_SECRET_ACCESS_KEY=${env.aws_secret_key}",
      'PATH+=/opt/tfenv/bin',
      "TF_LOG=${tf_log_level}"
    ]) {
      sh """
      export TFENV_DEBUG=1
      pwd
      bash -x /var/lib/jenkins/workspace/Recalls/recalls-build/recalls-infrastructure/bin/terraform.sh \
        --action ${action} \
        --project ${project} \
        --environment ${environment} \
        --component ${component} \
        --region ${region} \
        -- ${extra_args}
      """
    } //withEnv
  } //withCredentials
}

def get_tfenv() {
  if (!fileExists('/opt/tfenv/bin/tfenv')) {
    dir('/opt') { check_out_github_repo('cartest', 'tfenv', 'master') }
  } else {
    println '[INFO] TFENV already installed. Skipping...'
  }
}

aws_region = 'eu-west-1'
s3_deploy_bucket_prefix = 'uk.gov.dvsa.vehicle-recalls.'
bucket_exists = 0
//todo move to configuration of job?
ENV = 'int'
TF_LOG_LEVEL = 'WARN'
TF_BRANCH = 'feature-BL-6181-pipeline'

s3_deploy_bucket = s3_deploy_bucket_prefix + ENV

fake_smmt_dist = ''
vehicle_recalls_dist = ''

node('builder') {
  wrap([$class: 'BuildUser']) {
    def user = env.BUILD_USER
    wrap([$class: 'TimestamperBuildWrapper']) {
      wrap([$class: 'AnsiColorBuildWrapper', colorMapName: 'xterm']) {
        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'FB_AWS_CREDENTIALS', usernameVariable: 'aws_key_id', passwordVariable: 'aws_secret_key']]) {
          withEnv([
            "AWS_DEFAULT_REGION=${aws_region}",
            "AWS_ACCESS_KEY_ID=${env.aws_key_id}",
            "AWS_SECRET_ACCESS_KEY=${env.aws_secret_key}",
            "BUILDSTAMP=${env.BUILD_NUMBER}"
          ]) {
            stage('Verify S3 Bucket') {
              bucket_exists = sh(
                script: "aws s3 ls s3://${s3_deploy_bucket} --region ${aws_region} 2>&1 | grep -q -e 'NoSuchBucket' -e 'AccessDenied'",
                returnStatus: true
              )
              if (bucket_exists == 0) {
                echo "[INFO] Bucket '${s3_deploy_bucket}' not found."
                echo "[INFO] Creating Bucket"
                node('ctrl' && 'dev') {
                  checkout_gitlab_repo('vehicle-recalls', 'recalls-infrastructure', "${TF_BRANCH}", '313a82d3-f2e7-4787-837e-7517f3ce84eb')
                  get_tfenv()

                  //create S3 Bucket for Lambda
                  tf_scaffold('FB', "${TF_LOG_LEVEL}", 'plan', 'vehicle-recalls', "${ENV}", 'vehicle_recalls', "${aws_region}", "--target module.VehicleRecalls.aws_s3_bucket.vehicle_recalls  -var environment=${ENV}")
                  tf_scaffold('FB', "${TF_LOG_LEVEL}", 'apply', 'vehicle-recalls', "${ENV}", 'vehicle_recalls', "${aws_region}", "--target module.VehicleRecalls.aws_s3_bucket.vehicle_recalls  -var environment=${ENV}")
                }
              } else {
                echo "[INFO] Bucket '${s3_deploy_bucket}' found."
              }
            }

            stage('Build Fake SMMT') {
              sh("rm -rf vehicle-recalls-fake-smmt-service")
              checkout_github_repo("dvsa", "vehicle-recalls-fake-smmt-service", "master")
              dir("vehicle-recalls-fake-smmt-service") {
                dir("app") {
                  sh("npm install")
                  sh("npm run build")

                  dir("dist") {
                    sh("ls -lah")
                    def dist_files = sh_output("ls | wc -l").toInteger()
                    echo "$dist_files files in dist"
                    if (dist_files > 1) {
                      abort_build("More than one file in dist, aborting")
                    }

                    fake_smmt_dist = sh_output("ls")
                    copy_file_to_s3(fake_smmt_dist, s3_deploy_bucket)
                  }
                }
              }
            }

            stage('Build Vehicle Recalls API') {
              //we don't have lambda yet so we are downloading fake again
              checkout_github_repo("dvsa", "vehicle-recalls-fake-smmt-service", "master")
              dir("vehicle-recalls-fake-smmt-service") {
                dir("app") {
                  sh("npm install")
                  sh("npm run build")

                  dir("dist") {
                    sh("ls -lah")
                    def dist_files = sh_output("ls | wc -l").toInteger()
                    echo "$dist_files files in dist"
                    if (dist_files > 1) {
                      abort_build("More than one file in dist, aborting")
                    }

                    vehicle_recalls_dist = sh_output("ls")
                    sh("mv $vehicle_recalls_dist vehicle-recalls-beta.zip")
                    vehicle_recalls_dist = "vehicle-recalls-beta.zip"

                    copy_file_to_s3(vehicle_recalls_dist, s3_deploy_bucket)
                  }
                }
              }
            }

            stage('TF Plan & Apply') {
              node('ctrl' && 'dev') {
                get_tfenv()

                checkout_gitlab_repo('vehicle-recalls', 'recalls-infrastructure', "${TF_BRANCH}", '313a82d3-f2e7-4787-837e-7517f3ce84eb')

                  def vars = "-var environment=${ENV} " +
                             "-var lambda_fake_smmt_s3_key=${fake_smmt_dist} " +
                             "-var lambda_vehicle_recalls_api_s3_key=${vehicle_recalls_dist}"

                  tf_scaffold('FB', "${TF_LOG_LEVEL}", 'plan', 'vehicle-recalls', "${ENV}", 'vehicle_recalls', "${aws_region}", vars)
                  go_wait = 5
                  //if ("${ENV}" != "int" && destroy == 'false') {
                  echo "[INFO] You have ${go_wait} minutes to decide if you want to continue."
                  timeout(go_wait) { input message: 'Are you happy with the plan?', ok: 'Yes!' }
                  //}
                  tf_scaffold('FB', "${TF_LOG_LEVEL}", 'apply', 'vehicle-recalls', "${ENV}", 'vehicle_recalls', "${aws_region}", vars)
                  tf_scaffold('FB', "${TF_LOG_LEVEL}", 'output', 'vehicle-recalls', "${ENV}", 'vehicle_recalls', "${aws_region}", vars)
              }
            }

          }
        }
      }
    }
  }
}