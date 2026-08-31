pipeline {

    agent any

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Verify Environment') {
            steps {
                bat 'java -version'
                bat 'mvn -version'
            }
        }

        stage('Run API Tests') {
            steps {
                bat 'mvn clean test'
            }
        }
    }

    post {

        always {

            junit(
                testResults: '**/target/surefire-reports/*.xml',
                allowEmptyResults: true
            )

            archiveArtifacts(
                artifacts: 'logs/*.txt',
                allowEmptyArchive: true
            )
        }

        success {

            emailext(
                to: 'neelgaganat97@gmail.com',
                subject: "REST Assured API Tests - BUILD #${BUILD_NUMBER} - SUCCESS",
                body: """
Hello Neel,

Your REST Assured API automation build has completed successfully.

Project: ${JOB_NAME}
Build Number: ${BUILD_NUMBER}
Status: SUCCESS

The API tests were executed successfully through Jenkins.

Jenkins Build:
${BUILD_URL}

The execution log has been archived with this build.

Regards,
Jenkins
"""
            )
        }

        failure {

            emailext(
                to: 'neelgaganat97@gmail.com',
                subject: "REST Assured API Tests - BUILD #${BUILD_NUMBER} - FAILED",
                body: """
Hello Neel,

Your REST Assured API automation build has FAILED.

Project: ${JOB_NAME}
Build Number: ${BUILD_NUMBER}
Status: FAILED

Please check the Jenkins console output for the failure details.

Jenkins Build:
${BUILD_URL}

Regards,
Jenkins
""",
                attachLog: true
            )
        }
    }
}
