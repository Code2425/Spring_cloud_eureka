pipeline {
    agent any
    environment {
        EC2_HOST = '18.117.10.150'
        EC2_USER = 'ec2-user'
        APP_DIR = '/order-service'
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

        stage('Deploy to EC2') {
            steps {

                withCredentials([
                        sshUserPrivateKey(
                                credentialsId: 'ec2-deployment-key',
                                keyFileVariable: 'EC2_KEY',
                                usernameVariable: 'EC2_USER'
                        )
                ]) {

                    bat """
                scp -o StrictHostKeyChecking=no ^
                -i "%EC2_KEY%" ^
                eureka-server\\\\target\\\\eureka-server-1.0.0.jar ^
                %EC2_USER%@%EC2_HOST%:%APP_DIR%/
            """

                    bat """
                ssh -o StrictHostKeyChecking=no ^
                -i "C:\\Users\\kesar\\OneDrive\\Desktop\\AI\\AWS\\local-aws-key.pem" ^
                %EC2_USER%@%EC2_HOST% ^
                "sudo systemctl restart order-service"
            """
                }
            }
        }
    }
}