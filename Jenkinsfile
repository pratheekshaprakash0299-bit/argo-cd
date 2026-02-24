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

        stage('Checkout (Select Branch From Dropdown)') {
            steps {
                git branch: "${params.BRANCH}",
                    url: 'https://github.com/pratheekshaprakash0299-bit/argo-cd.git'
            }
        }

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

        stage('Quality Gate') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: false
                }
            }
        }

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

    stage('Update K8s Manifest & Push to GitHub') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'github-creds',
                    usernameVariable: 'GIT_USER',
                    passwordVariable: 'GIT_PASS'
                )]) {

                    sh """
                        echo "Updating deployment.yaml with new image tag"

                        git config user.email "jenkins@local"
                        git config user.name "jenkins"

                        sed -i 's|image:.*|image: ${IMAGE_URI}:${IMAGE_TAG}|g' k8s/deployment.yaml

                        git add k8s/deployment.yaml
                        git commit -m "Updated image to ${IMAGE_TAG}"
                        
                        git push https://${GIT_USER}:${GIT_PASS}@github.com/pratheekshaprakash0299-bit/argo-cd.git HEAD:${params.BRANCH}
                    """
                }
            }
        }
    }

    post {
        success {
            echo "Pipeline executed successfully! ArgoCD will deploy automatically."
        }
        failure {
            echo "Pipeline failed!"
        }
    }
}
