pipeline {
    agent any

    tools {
        maven 'Maven 3'
        jdk 'JDK 21'
    }

    environment {
        MAVEN_OPTS = '-Xmx1024m'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Clean & Compile') {
            steps {
                bat 'mvn clean compile test-compile'
            }
        }

        stage('Run Tests') {
            steps {
                bat 'mvn test'
            }
        }

        stage('Publish Reports') {
            steps {
                script {
                    // Publish TestNG results
                    step([
                        $class: 'Publisher',
                        reportFilenamePattern: '**/target/surefire-reports/testng-results.xml'
                    ])
                }
            }
        }
    }

    post {
        always {
            // Archive test logs
            archiveArtifacts artifacts: 'logs/*.txt', allowEmptyArchive: true

            // Publish JUnit-style TestNG reports
            junit testResults: 'target/surefire-reports/junitreports/*.xml', allowEmptyResults: true

            // Clean workspace
            cleanWs()
        }

        success {
            echo '✅ All API tests passed successfully!'
        }

        failure {
            echo '❌ Some tests failed. Check the reports for details.'
        }
    }
}
