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
            echo '======================================'
            echo 'API AUTOMATION TESTS PASSED'
            echo '======================================'
        }

        failure {
            echo '======================================'
            echo 'API AUTOMATION TESTS FAILED'
            echo '======================================'
        }
    }
}
