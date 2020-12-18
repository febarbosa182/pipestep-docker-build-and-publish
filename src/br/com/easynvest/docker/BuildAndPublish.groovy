package br.com.easynvest.publish_image

class PublishImage{
    def call (jenkins) {

        jenkins.podTemplate(
            yaml: """
apiVersion: v1
kind: Pod
metadata:
  name: docker
  labels:
    app : docker
spec:
  containers:
  - name: docker
    image: docker:20.10.0
    tty: true
    imagePullPolicy: IfNotPresent
    command:
      - /bin/sh
    volumeMounts:
    - name: dockersock
      mountPath: "/var/run/docker.sock"
  volumes:
  - name: dockersock
    hostPath:
      path: /var/run/docker.sock
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

                    jenkins.docker.build("\${DOCKER_IMAGE}:\${APP_VERSION}.\${GIT_COMMIT}","--network=host .")
                    // jenkins.docker.image("\${DOCKER_IMAGE}:\${APP_VERSION}.\${GIT_COMMIT}").push()
                }
            }
        }
    }
}