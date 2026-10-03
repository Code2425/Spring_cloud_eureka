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

                    bat '''
      scp -o StrictHostKeyChecking=no -i "C:\\ProgramData\\Jenkins\\.ssh\\ec2-key.pem" ^
        eureka-server\\target\\eureka-server-1.0.0.jar ^
        ec2-user@18.117.10.150:/order-service/
      ssh -o StrictHostKeyChecking=no -i "C:\\ProgramData\\Jenkins\\.ssh\\ec2-key.pem" ^
        ec2-user@18.117.10.150 "pkill -f eureka-server; nohup java -jar /order-service/eureka-server-1.0.0.jar > /order-service/app.log 2>&1 &"
    '''
                }
            }
        }
    }
}