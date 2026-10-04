pipeline {
    agent any
    environment {
        EC2_HOST = '18.188.48.129'
        EC2_USER = 'ec2-user'
        APP_DIR  = '/order-service'
        EC2_KEY  = 'C:\\ProgramData\\Jenkins\\.ssh\\ec2-key.pem'
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
                // No withCredentials here anymore — see note 1 below.
                // Copies every service jar, then restarts the systemd services.
                bat '''
                    scp -o StrictHostKeyChecking=no -i "%EC2_KEY%" eureka-server\\target\\eureka-server-*.jar %EC2_USER%@%EC2_HOST%:%APP_DIR%/
                    scp -o StrictHostKeyChecking=no -i "%EC2_KEY%" product-service\\target\\product-service-*.jar %EC2_USER%@%EC2_HOST%:%APP_DIR%/
                    scp -o StrictHostKeyChecking=no -i "%EC2_KEY%" order-service\\target\\order-service-*.jar %EC2_USER%@%EC2_HOST%:%APP_DIR%/
                    scp -o StrictHostKeyChecking=no -i "%EC2_KEY%" gateway-service\\target\\gateway-service-*.jar %EC2_USER%@%EC2_HOST%:%APP_DIR%/
                    ssh -o StrictHostKeyChecking=no -i "%EC2_KEY%" %EC2_USER%@%EC2_HOST% ^
                      "sudo systemctl restart eureka-server product-service order-service gateway-service"
                '''
            }
        }
    }

    post {
        failure {
            echo 'Build failed. On EC2, check: sudo journalctl -u eureka-server -n 50'
        }
    }
}
