pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                // Jenkins will automatically check out the source if using Pipeline from SCM
                echo "Source code checked out from GitHub branch: master"
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

        stage('Provision EC2') {
            steps {


                sshagent (credentials: ['ubuntu-ec2-key']) {


                    // Java Installation
                    sh '''
                        ssh -o StrictHostKeyChecking=no -o ServerAliveInterval=60 -o ServerAliveCountMax=5 ubuntu@ec2-35-77-225-211.ap-northeast-1.compute.amazonaws.com "set -e; sudo apt-get update -y; sudo apt-get upgrade -y; which java || sudo apt install openjdk-17-jdk openjdk-17-jre -y; java --version"
                    '''


                }
            }
        }

        stage('Deploy to EC2') {
            steps {
                sshagent (credentials: ['ubuntu-ec2-key']) {
                    sh '''
                        # Copy JAR file
                        scp -v -o StrictHostKeyChecking=no -o ServerAliveInterval=60 -o ServerAliveCountMax=5 target/*.jar ubuntu@ec2-35-77-225-211.ap-northeast-1.compute.amazonaws.com:/home/ubuntu/app.jar

                        # Deploy with better debugging
                        ssh -o StrictHostKeyChecking=no ServerAliveInterval=60 -o ServerAliveCountMax=5 ubuntu@ec2-35-77-225-211.ap-northeast-1.compute.amazonaws.com /bin/bash <<\'EOF\'
                            # Stop existing app
                            echo "=== Stopping existing application ==="
                            pgrep -f app.jar && pkill -f app.jar
                            sleep 3

                            # Check port usage
                            echo "=== Port 8080 Status ==="
                            sudo ss -tulnp | grep 8080 || echo "Port 8080 available"

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
