package dynamic.publish_image

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
                - name: docker
                  image: docker:20.10
                  command:
                  - sleep
                  args:
                  - 99d
                  volumeMounts:
                  - name: docker-socket
                    mountPath: /var/run
                - name: docker-daemon
                  image: docker:20.10-dind
                  securityContext:
                    privileged: true
                  volumeMounts:
                  - name: docker-socket
                    mountPath: /var/run
            """,
            yamlMergeStrategy: jenkins.merge(),
            workspaceVolume: jenkins.persistentVolumeClaimWorkspaceVolume(
                claimName: "pvc-workspace-${jenkins.env.JENKINS_AGENT_NAME}",
                readOnly: false
            )
        )
        {
            jenkins.node(jenkins.POD_LABEL){
                jenkins.container('docker') {
                    jenkins.echo "Build and Publish Docker image Step"
                    def packageJson = jenkins.readJSON file: 'package.json'
                    jenkins.env.APP_VERSION = packageJson.version
                    // WE WILL NOT PUSH SINCE IT'S A LOCAL STACK
                    jenkins.sh label: "Build image and publish multi architecture", script: """
                        docker buildx create --use
                        docker buildx build --platform linux/amd64,linux/arm64,linux/arm/v7 -t \${DOCKER_IMAGE}:\${APP_VERSION}.\${GIT_COMMIT} .
                    """
                }
            }
        }
    }
}