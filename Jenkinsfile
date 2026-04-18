pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        timestamps()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Test') {
            steps {
                script {
                    if (isUnix()) {
                        sh './mvnw test'
                    } else {
                        bat 'mvnw.cmd test'
                    }
                }
            }
        }

        stage('Package') {
            steps {
                script {
                    if (isUnix()) {
                        sh './mvnw -DskipTests package'
                    } else {
                        bat 'mvnw.cmd -DskipTests package'
                    }
                }
            }
        }

        stage('Helm') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'helm lint ./helm/my-bank'
                        sh 'helm template my-bank ./helm/my-bank > /dev/null'
                    } else {
                        bat 'helm lint .\\helm\\my-bank'
                        bat 'helm template my-bank .\\helm\\my-bank > NUL'
                    }
                }
            }
        }
    }
}
