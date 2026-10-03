pipeline {
    agent any
    environment {
        EC2_HOST = '18.117.10.150'
        EC2_USER = 'ec2-user'
        APP_DIR = '/opt/order-service'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                bat 'mvn clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                bat 'mvn test'
            }
        }

        stage('Deploy JAR to EC2') {
            steps {

                withCredentials([
                        sshUserPrivateKey(
                                credentialsId: 'ec2-deployment-key',
                                keyFileVariable: 'EC2_KEY',
                                usernameVariable: 'EC2_SSH_USER'
                        )
                ]) {

                    bat """
                        scp -o StrictHostKeyChecking=no ^
                        -i "%EC2_KEY%" ^
                        target\\eureka-server-1.0.0.jar ^
                        %EC2_SSH_USER%@%EC2_HOST%:%APP_DIR%/
                    """
                }
            }
        }
    }
}