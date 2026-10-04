pipeline {
    agent any
    environment {
        DOCKERHUB = credentials('dockerhub-creds')   // Jenkins username/password credential
        IMAGE_TAG = "${env.BUILD_NUMBER}"
    }
    stages {
        stage('Checkout') { steps { checkout scm } }
        stage('Build')    { steps { bat 'mvn clean package -DskipTests' } }
        stage('Test')     { steps { bat 'mvn test' } }
        stage('Docker Build & Push to Docker Hub') {
            steps {
                bat '''
                    docker login -u "%DOCKERHUB_USR%" -p "%DOCKERHUB_PSW%" >nul 2>&1
                    for %%s in (eureka-server product-service order-service gateway-service) do (
                        docker build -t %DOCKERHUB_USR%/%%s:%IMAGE_TAG% ./%%s
                        docker push %DOCKERHUB_USR%/%%s:%IMAGE_TAG%
                    )
                '''
            }
        }
        stage('Deploy to KIND') {
            steps {
                bat '''
                    kubectl config use-context kind-dev
                    powershell -Command "(Get-ChildItem k8s/*.yaml) | ForEach-Object { (Get-Content $_.FullName).replace('DOCKERHUB_USER', $env:DOCKERHUB_USR) | Set-Content $_.FullName }"
                    kubectl apply -f k8s/
                    for %%s in (eureka-server product-service order-service gateway-service) do (
                        kubectl set image deployment/%%s %%s=%DOCKERHUB_USR%/%%s:%IMAGE_TAG%
                    )
                    kubectl rollout status deployment/eureka-server --timeout=240s
                    kubectl get pods
                '''
            }
        }
    }
}
