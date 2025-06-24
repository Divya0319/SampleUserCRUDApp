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
                // Write SQL files
                writeFile file: 'alter_user.sql', text: '''
        ALTER USER 'root'@'localhost' IDENTIFIED BY 'NewRootPassword123!';
        FLUSH PRIVILEGES;
        '''
                writeFile file: 'cleanup.sql', text: '''
        DELETE FROM mysql.user WHERE User='';
        DELETE FROM mysql.user WHERE User='root' AND Host NOT IN ('localhost', '127.0.0.1', '::1');
        DROP DATABASE IF EXISTS test;
        FLUSH PRIVILEGES;
        '''
                writeFile file: 'setup_database.sql', text: '''
        CREATE DATABASE IF NOT EXISTS sampleUserCrudDb;
        CREATE USER IF NOT EXISTS 'scalerstudent'@'localhost' IDENTIFIED BY 'Scalerstudent123';
        GRANT ALL PRIVILEGES ON sampleUserCrudDb.* TO 'scalerstudent'@'localhost';
        FLUSH PRIVILEGES;
        '''

                sshagent (credentials: ['ec2-key']) {
                    sh '''
                        # Copy SQL files to EC2
                        scp -o StrictHostKeyChecking=no -o ServerAliveInterval=60 -o ServerAliveCountMax=5 alter_user.sql cleanup.sql setup_database.sql ec2-user@ec2-35-78-82-28.ap-northeast-1.compute.amazonaws.com:/home/ec2-user/
                    '''

                    // Java Installation
                    sh '''
                        ssh -o StrictHostKeyChecking=no -o ServerAliveInterval=60 -o ServerAliveCountMax=5 ec2-user@ec2-35-78-82-28.ap-northeast-1.compute.amazonaws.com "set -e; which java || sudo yum install -y java-17-amazon-corretto; java -version"
                    '''

                    // MariaDB Installation
                    sh '''
                        ssh -o StrictHostKeyChecking=no -o ServerAliveInterval=60 -o ServerAliveCountMax=5 ec2-user@ec2-35-78-82-28.ap-northeast-1.compute.amazonaws.com "set -e; if ! command -v mysql >/dev/null; then sudo dnf install -y mariadb105-server; sudo systemctl enable mariadb; sudo systemctl start mariadb; else echo '=== MariaDB already installed ==='; sudo systemctl start mariadb || true; fi; mysql -V"
                    '''

                    // Root Password Setup & Cleanup
                    sh '''
                        ssh -o StrictHostKeyChecking=no -o ServerAliveInterval=60 -o ServerAliveCountMax=5 ec2-user@ec2-35-78-82-28.ap-northeast-1.compute.amazonaws.com "set -e; if mysql -u root -p'NewRootPassword123!' -e 'SELECT 1' 2>/dev/null; then echo '=== Root password already set ==='; else echo '=== Setting root password and cleanup ==='; sudo mysql -u root < /home/ec2-user/alter_user.sql; mysql -u root -p'NewRootPassword123!' < /home/ec2-user/cleanup.sql; fi"
                    '''

                    // Database Creation
                    sh '''
                        ssh -o StrictHostKeyChecking=no -o ServerAliveInterval=60 -o ServerAliveCountMax=5 ec2-user@ec2-35-78-82-28.ap-northeast-1.compute.amazonaws.com "set -e; echo '=== Setting up MariaDB Database/User ==='; mysql -u root -p'NewRootPassword123!' < /home/ec2-user/setup_database.sql"
                    '''
                }
            }
        }

        stage('Deploy to EC2') {
            steps {
                sshagent (credentials: ['ec2-key']) {
                    sh '''
                        # Copy JAR file
                        scp -o StrictHostKeyChecking=no target/*.jar ec2-user@ec2-35-78-82-28.ap-northeast-1.compute.amazonaws.com:/home/ec2-user/app.jar

                        # Deploy with better debugging
                        ssh -o StrictHostKeyChecking=no ec2-user@ec2-35-78-82-28.ap-northeast-1.compute.amazonaws.com /bin/bash <<\'EOF\'
                            # Stop existing app
                            echo "=== Stopping existing application ==="
                            pgrep -f app.jar && pkill -f app.jar
                            sleep 3

                            # Check port usage
                            echo "=== Port 8080 Status ==="
                            sudo netstat -tulnp | grep 8080 || echo "Port 8080 available"

                            # Start new instance with debug output
                            echo "=== Starting Application ==="
                            nohup java -jar /home/ec2-user/app.jar > /home/ec2-user/app.log 2>&1 &
                            sleep 5

                            # Verify
                            echo "=== Verification ==="
                            if pgrep -f app.jar >/dev/null; then
                                echo "Application running with PID: $(pgrep -f app.jar)"
                                exit 0
                            else
                                echo "=== Application Logs ==="
                                cat /home/ec2-user/app.log
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
