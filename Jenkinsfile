pipeline {
    agent {
        node { label 'linux&&docker' }
    }

    stages {

        stage('Create the docker builder image(s)') {
            steps {
                script {
                    jdk8BuilderImage = docker.build("jdk8-maven:${env.BUILD_ID}", "-f jdk8.build.Dockerfile .")
                }
            }
        }

        stage('Build a branch') {
            when { not { branch 'develop' } }
            steps {
                //Sometime in the future these maven-settings should not be needed here (model service should build without access to SDL repositories)
                withCredentials([file(credentialsId: 'dxa-maven-settings', variable: 'MAVEN_SETTINGS_PATH')]) {
                    script {
                        //Build on JDK8
                        jdk8BuilderImage.inside {
                            //Build CIL version:
                            sh "mvn -s $MAVEN_SETTINGS_PATH -Pcil -Psonatype-repository -B clean install"

                            //Build in-process version:
                            sh "mvn -s $MAVEN_SETTINGS_PATH -Pin-process -Psonatype-repository -B clean install"
                        }
                    }
                }
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
                success {
                    archiveArtifacts artifacts: "dxa-model-service-assembly-in-process/target/dxa-model-service/standalone-in-process/**,dxa-model-service-assembly/target/dxa-model-service/standalone/**"
                }
            }
        }

        stage('Build and deploy from develop') {
            when { branch 'develop' }
            steps {
                withCredentials([file(credentialsId: 'dxa-maven-settings', variable: 'MAVEN_SETTINGS_PATH')]) {
                    script {
                        //Build on JDK8 and deploy it to local repository:
                        jdk8BuilderImage.inside {
                            //Build CIL version:
                            sh "mvn -B -s $MAVEN_SETTINGS_PATH -Pcil -Plocal-repository clean source:jar deploy"

                            //Build in-process version:
                            sh "mvn -B -s $MAVEN_SETTINGS_PATH -Pin-process -Plocal-repository clean source:jar deploy"
                        }
                    }
                }
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
                success {
                    archiveArtifacts artifacts: "dxa-model-service-assembly-in-process/target/dxa-model-service/standalone-in-process/**,dxa-model-service-assembly/target/dxa-model-service/standalone/**"
                }
            }
        }
    }
}
