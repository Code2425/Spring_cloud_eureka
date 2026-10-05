// Declarative pipeline definition — Jenkins reads this file to know what to run, in what order
pipeline {
    // Run on any available Jenkins agent (your local Windows machine, in this setup)
    agent any
    // Environment variables available to EVERY stage; Jenkins injects these into each bat/powershell step
    environment {
        // Jenkins "Username with password" credential with ID 'dockerhub-creds' — exposes DOCKERHUB_USR and DOCKERHUB_PSW, masked as **** in logs
        DOCKERHUB = credentials('dockerhub-creds')   // Jenkins username/password credential
        // Immutable image tag from the Jenkins build number — build #33 always means image :33, traceable forever
        IMAGE_TAG = "${env.BUILD_NUMBER}"
    }
    // The ordered list of phases; a failure in any stage stops the pipeline (fail fast)
    stages {
        // Stage 1: pull the code — 'checkout scm' uses the repo/branch the Jenkins job itself is configured with
        stage('Checkout') { steps { checkout scm } }
        // Stage 2: compile all modules and package the jars, skipping tests for speed (tests run properly in the next stage)
        stage('Build')    { steps { bat 'mvn clean package -DskipTests' } }
        // Stage 3: run the unit tests — fails the build here if any test breaks, so broken code never becomes an image
        stage('Test')     { steps { bat 'mvn test' } }
        // Stage 4: containerize — turn each module's jar into a Docker image and publish it
        stage('Docker Build & Push to Docker Hub') {
            steps {
                bat '''
                    // Log in to Docker Hub once; >nul 2>&1 hides the output so the token never appears in logs (Jenkins masks it too — belt and suspenders)
                    docker login -u "%DOCKERHUB_USR%" -p "%DOCKERHUB_PSW%" >nul 2>&1
                    // Windows batch loop over the 4 service dirs; %%s is the loop variable (doubled % because this runs from a .bat file Jenkins generates)
                    for %%s in (eureka-server product-service order-service gateway-service) do (
                        // Build the image from ./%%s (that module's Dockerfile + target/*.jar) and name it <username>/<service>:<build#>
                        docker build -t %DOCKERHUB_USR%/%%s:%IMAGE_TAG% ./%%s
                        // Push it to Docker Hub — this is what makes the image pullable by the KIND cluster
                        docker push %DOCKERHUB_USR%/%%s:%IMAGE_TAG%
                    )
                '''
            }
        }
        // Stage 5: deploy — everything above built artifacts; this stage puts them on the cluster
        stage('Deploy to KIND') {
            steps {
                bat '''
                    // Point kubectl at the KIND cluster (Jenkins may have several kubeconfig contexts; this pins the right one)
                    kubectl config use-context kind-dev
                    // Replace the DOCKERHUB_USER placeholder in k8s/*.yaml with your real Hub username — plain text substitution before apply
                    powershell -Command "(Get-ChildItem k8s/*.yaml) | ForEach-Object { (Get-Content $_.FullName).replace('DOCKERHUB_USER', $env:DOCKERHUB_USR) | Set-Content $_.FullName }"
                    // Create/update all Deployments + Services from the manifests (idempotent — safe to re-run)
                    kubectl apply -f k8s/
                    // Pin each Deployment to THIS build's immutable image tag — this is what actually triggers the rolling update to the new images
                    for %%s in (eureka-server product-service order-service gateway-service) do (
                        kubectl set image deployment/%%s %%s=%DOCKERHUB_USR%/%%s:%IMAGE_TAG%
                    )
                    // Block up to 4 minutes until eureka-server's rollout finishes — fails the stage if pods never become ready, instead of silently deploying broken pods
                    kubectl rollout status deployment/eureka-server --timeout=240s
                    // Show the final pod table in the log as a visual confirmation of what got deployed
                    kubectl get pods
                '''
            }
        }
    }
}
