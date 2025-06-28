pipeline {
    agent any

    environment {
            AWS_ACCESS_KEY_ID = credentials('AWS_ACCESS_KEY_ID')
            AWS_SECRET_ACCESS_KEY = credentials('AWS_SECRET_ACCESS_KEY')
            AWS_DEFAULT_REGION = 'ap-northeast-1'
            S3_BUCKET = 'bkt-sample-app-artifacts'
            S3_KEY = 'app.jar'
            EC2_USER = 'ubuntu'
            EC2_HOST = 'ec2-54-238-200-192.ap-northeast-1.compute.amazonaws.com'
    }

    stages {

        stage('Shell Test') {
            steps {
                sh 'echo "Shell is working fine"'
            }
        }

        stage('Checkout') {
            steps {
                // Jenkins will automatically check out the source if using Pipeline from SCM
                echo "Source code checked out from GitHub branch: dep-ubuntu"
            }
        }

        stage('Build') {
            steps {
                echo "Building the project..."
                sh 'chmod +x mvnw'
                sh './mvnw clean package -DskipTests'
            }
        }

        stage('Package Info') {
            steps {
                echo "Listing target folder contents..."
                sh 'ls -lh target'
            }
        }


        stage('Archive') {
            steps {
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        stage('Upload to S3') {
            steps {
                echo 'Uploading JAR to S3...'
                sh 'aws s3 cp target/*.jar s3://${S3_BUCKET}/${S3_KEY}'
            }
        }


        stage('Provision EC2') {
            steps {

                sshagent (credentials: ['ec2-key']) {

                    // Java Installation
                    sh '''
                        ssh -o StrictHostKeyChecking=no -o ServerAliveInterval=60 -o ServerAliveCountMax=5 ${EC2_USER}@${EC2_HOST} "set -e; sudo apt-get update -y; sudo apt-get upgrade -y; which java || sudo apt install openjdk-17-jdk openjdk-17-jre -y; java --version"
                    '''

                }
            }
        }

        stage('Deploy to EC2') {
            steps {
                sshagent (credentials: ['ec2-key']) {
                    echo 'Deploying on EC2...'
                    sh '''
                        echo "Pulling latest JAR from S3..."
                        ssh -o StrictHostKeyChecking=no -o ServerAliveInterval=60 -o ServerAliveCountMax=5 ${EC2_USER}@${EC2_HOST} /bin/bash <<EOF
                            echo "Pulling latest JAR from S3..."
                            aws s3 cp s3://${S3_BUCKET}/${S3_KEY} /home/ubuntu/app.jar
EOF


                        # Deploy with better debugging
                        set -e
                        ssh -o StrictHostKeyChecking=no -o ServerAliveInterval=60 -o ServerAliveCountMax=5 ${EC2_USER}@${EC2_HOST} /bin/bash <<\'EOF\'
                            # Stop existing app
                            echo "=== Stopping existing application ==="
                            pgrep -f app.jar && pkill -f app.jar
                            sleep 3

                            # Check port usage
                            echo "=== Port 8100 Status ==="
                            sudo ss -tulnp | grep 8100 || echo "Port 8100 available"

                            # Start new instance with debug output
                            echo "=== Starting Application ==="
                            nohup java -jar /home/ubuntu/app.jar > /home/ubuntu/app.log 2>&1 &
                            sleep 5

                            # Verify
                            echo "=== Verification ==="
                            if pgrep -f app.jar >/dev/null; then
                                echo "Application running with PID: $(pgrep -f app.jar)"
                                exit 0
                            else
                                echo "=== Application Logs ==="
                                cat /home/ubuntu/app.log
                                echo "ERROR: Process failed to start"
                                exit 1
                            fi
EOF
                    '''
                }
            }
        }
    }

    post {
        success {
            echo '✅ Build and tests succeeded!'
        }
        failure {
            echo '❌ Build failed. Check logs above.'
        }
    }
}
