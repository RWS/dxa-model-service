pipeline {
    agent {
        node { label 'linux&&docker' }
    }

    stages {
/*
	//Sometime in the future we should be able to build on JDK11:
        stage ('Build with JDK11') {
            steps {
                //DXA has to be able to be built on JDK11:
                withDockerContainer("maven:3.6-jdk-11-slim") { 
                    //DXA has to be able to be build without SDL proprietary dependencies:
                    sh "mvn -B dependency:purge-local-repository -DreResolve=false"

                    sh "mvn -B clean verify"
                }
            }
        }
*/

        stage('Create the docker builder image(s)') {
            steps {
                script {
                    def jdk8BuilderImage = docker.build("jdk8-maven:${env.BUILD_ID}", "-f jdk8.build.Dockerfile .")
                }
            }
        }

        stage('Build a branch') {
            when { not { branch 'develop' } }
            // Not on the develop branch, so build it, but do not install it.
            steps {
                //Sometime in the future these maven-settings should not be needed here (model service should build without acces to SDL repositories)
                withCredentials([file(credentialsId: 'dxa-maven-settings', variable: 'MAVEN_SETTINGS_PATH')]) {
                    script {
                        //Build on JDK8 and deploy it to local repository:
                        jdk8BuilderImage.inside {
                            sh "mvn -s $MAVEN_SETTINGS_PATH -B clean verify"
                        }
                    }
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
                            sh "mvn -B -s $MAVEN_SETTINGS_PATH -Plocal-repository clean source:jar deploy"
                        }
                    }
                }
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
    }
}
