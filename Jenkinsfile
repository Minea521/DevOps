pipeline {
    stages {
        stage('Build') {
            steps {
                echo '=== Building Laravel Project ==='
                checkout scm

                echo 'Setting up environment...'
                sh 'cp .env.example .env'

                echo 'Installing dependencies...'
                sh 'composer install'
                sh 'npm install'

                echo 'Generating application key...'
                sh 'php artisan key:generate'
            }
        }

        stage('Test') {
            steps {
                echo '=== Running Tests ==='
                sh 'php artisan test'
            }
        }

        stage('Deploy') {
            steps {
                echo '=== Deploying to Server ==='
                sh 'ansible-playbook -i inventory/hosts.ini deploy.yml'
                sh 'echo "Deployment completed successfully"'
            }
        }
    }

    post {
        success {
            echo '✅ Pipeline completed successfully!'
        }
        failure {
            echo '❌ Pipeline failed!'
        }
    }
}