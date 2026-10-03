pipeline {
    agent any
    environment {
        EC2_HOST = '18.117.10.150'
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
        stage('Smoke Test') {
            steps {
                // Waits up to ~2 min for Eureka, then fails the build if it's not up.
                bat '''
                    set /a tries=0
                    :wait_eureka
                    curl -sf http://%EC2_HOST%:8761/ >nul && goto eureka_up
                    set /a tries+=1
                    if %tries% geq 12 exit /b 1
                    powershell -Command "Start-Sleep -Seconds 10"
                    goto wait_eureka
                    :eureka_up
                    echo Eureka is UP — open http://%EC2_HOST%:8761 in your browser
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
