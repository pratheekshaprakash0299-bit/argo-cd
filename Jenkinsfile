pipeline {
    agent any

    parameters {
        choice(
            name: 'BRANCH',
            choices: ['main', 'dev', 'test'],
            description: 'Select Branch'
        )
    }

    environment {
        AWS_REGION = "ap-south-1"
        ACCOUNT_ID = "220309168382"
        ECR_REPO = "sonarqube-project"
        IMAGE_REPO = "${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO}"
        IMAGE_TAG = "${BUILD_NUMBER}"
        PROJECT_KEY = "sonarqube-project"
    }

    stages {

        // ================= CHECKOUT =================
        stage('Checkout (Select Branch From Dropdown)') {
            steps {
                git branch: "${params.BRANCH}",
                    url: 'https://github.com/pratheekshaprakash0299-bit/argo-cd.git'
            }
        }

        // ================= SONAR =================
        stage('Sonar Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    withCredentials([
                        string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')
                    ]) {
                        sh '''
                            mvn clean verify sonar:sonar \
                              -Dsonar.projectKey=sonarqube-project \
                              -Dsonar.projectName=sonarqube-project \
                              -Dsonar.token=$SONAR_TOKEN
                        '''
                    }
                }
            }
        }

        // ================= QUALITY GATE =================
        stage('Quality Gate') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: false
                }
            }
        }

        // ================= DOCKER BUILD =================
        stage('Build Docker Image') {
            steps {
                sh """
                    echo "Building Docker image with tag ${IMAGE_TAG}"
                    docker build -t ${IMAGE_REPO}:${IMAGE_TAG} .
                    
                    echo "Tagging image as latest"
                    docker tag ${IMAGE_REPO}:${IMAGE_TAG} ${IMAGE_REPO}:latest
                """
            }
        }

        // ================= LOGIN TO ECR USING AWS ACCESS KEY =================
        stage('Login to ECR') {
            steps {
                withCredentials([[
                    $class: 'AmazonWebServicesCredentialsBinding',
                    credentialsId: 'aws-ecr-credentials'
                ]]) {
                    sh """
                        aws ecr get-login-password --region ${AWS_REGION} | \
                        docker login --username AWS --password-stdin ${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com
                    """
                }
            }
        }

        // ================= PUSH IMAGE =================
        stage('Push Image to ECR') {
            steps {
                sh """
                    echo "Pushing image with build number tag ${IMAGE_TAG}"
                    docker push ${IMAGE_REPO}:${IMAGE_TAG}

                    echo "Pushing image with latest tag"
                    docker push ${IMAGE_REPO}:latest
                """
            }
        }
    }

    post {
        success {
            echo "Pipeline executed successfully!"
        }
        failure {
            echo "Pipeline failed!"
        }
    }
}
