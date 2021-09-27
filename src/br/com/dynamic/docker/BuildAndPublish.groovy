package br.com.dynamic.publish_image

class PublishImage{
    def call (jenkins) {

        jenkins.podTemplate(
          yaml: """
apiVersion: v1
kind: Pod
spec:
  volumes:
  - name: docker-socket
    emptyDir: {}
  containers:
  - name: aws-cli
    image: amazon/aws-cli:2.2.27
    ttyEnabled: true
    command:
    - sleep
    args:
    - 99d
  - name: docker
    image: docker:20.10.7
    command:
    - sleep
    args:
    - 99d
    volumeMounts:
    - name: docker-socket
      mountPath: /var/run
  - name: docker-daemon
    image: docker:20.10.7-dind
    securityContext:
      privileged: true
    volumeMounts:
    - name: docker-socket
      mountPath: /var/run
            """,
          yamlMergeStrategy: jenkins.merge(),
          workspaceVolume: jenkins.persistentVolumeClaimWorkspaceVolume(
              claimName: "pvc-${jenkins.env.JENKINS_AGENT_NAME}",
              readOnly: false
          )
        ) 
        {
            jenkins.node(jenkins.POD_LABEL){
              jenkins.container('docker'){
                jenkins.echo "Build and Publish Docker image Step"
                jenkins.docker.withRegistry("https://public.ecr.aws/i8c9l4d5"){
                  // jenkins.docker.build("\${DOCKER_IMAGE}:\${APP_VERSION}.\${GIT_COMMIT}")
                  jenkins.sh script:"DOCKER_BUILDKIT=1 docker build --progress plain -t \${DOCKER_IMAGE}:\${APP_VERSION}.\${GIT_COMMIT}.\${BUILD_NUMBER} .",
                              label: "Building docker"
                  jenkins.docker.image("\${DOCKER_IMAGE}:\${APP_VERSION}.\${GIT_COMMIT}.\${BUILD_NUMBER}").push()
                }
              }
            }
        }
    }
}