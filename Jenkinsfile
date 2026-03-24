@Library('gbif-common-jenkins-pipelines') _

pipeline {
  agent any

  tools {
    maven 'Maven 3.9.9'
    jdk 'OpenJDK17'
  }

  options {
    buildDiscarder(logRotator(numToKeepStr: '5'))
    skipStagesAfterUnstable()
    timestamps()
    disableConcurrentBuilds()
  }

  triggers {
    snapshotDependencies()
  }

  parameters {
    separator(name: 'release_separator', sectionHeader: 'Release Main Project Parameters')
    booleanParam(name: 'RELEASE', defaultValue: false, description: 'Do a Maven release')
    string(name: 'RELEASE_VERSION', defaultValue: '', description: 'Release version (optional)')
    string(name: 'DEVELOPMENT_VERSION', defaultValue: '', description: 'Development version (optional)')
    booleanParam(name: 'DRY_RUN_RELEASE', defaultValue: false, description: 'Dry Run Maven release')
  }

  stages {
    stage('Maven build') {
      when {
        allOf {
          not {
            expression { params.RELEASE }
          }
          not {
            anyOf {
              branch 'dev'
              branch 'master'
            }
          }
        }
      }
      steps {
        withMaven(
          globalMavenSettingsConfig: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709',
          mavenOpts: '-Xms2048m -Xmx8192m',
          mavenSettingsConfig: 'org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig1396361652540',
          traceability: true
        ) {
          sh '''
            mvn -B -Denforcer.skip=true clean install -T 1C \
              -Dparallel=classes -DuseUnlimitedThreads=true -U
          '''
        }
      }
    }

    stage('Maven snapshot deploy') {
      when {
        allOf {
          not {
            expression { params.RELEASE }
          }
          anyOf {
            branch 'dev'
            branch 'master'
          }
        }
      }
      steps {
        withMaven(
          globalMavenSettingsConfig: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709',
          mavenOpts: '-Xms2048m -Xmx8192m',
          mavenSettingsConfig: 'org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig1396361652540',
          traceability: true
        ) {
          sh '''
            mvn -B -Denforcer.skip=true clean deploy -T 1C \
              -Dparallel=classes -DuseUnlimitedThreads=true -U
          '''
        }
      }
    }

    stage('Maven release') {
      when {
        allOf {
          expression { params.RELEASE }
          branch 'master'
        }
      }
      environment {
        RELEASE_ARGS = utils.createReleaseArgs(
          params.RELEASE_VERSION,
          params.DEVELOPMENT_VERSION,
          params.DRY_RUN_RELEASE
        )
      }
      steps {
        withMaven(
          globalMavenSettingsConfig: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709',
          mavenOpts: '-Xms2048m -Xmx8192m',
          mavenSettingsConfig: 'org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig1396361652540',
          traceability: true
        ) {
          sh '''
            mvn -B release:prepare release:perform -T 1C \
              -Dparallel=classes -DuseUnlimitedThreads=true $RELEASE_ARGS
          '''
        }
      }
    }
  }

  post {
    success {
      echo 'Pipeline executed successfully!'
    }
    failure {
      echo 'Pipeline execution failed!'
    }
  }
}
