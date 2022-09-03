package dynamic.publish_image

class PublishImage{
    def call (jenkins) {

        jenkins.podTemplate(
            yaml: """
apiVersion: v1
kind: Pod
metadata:
  name: kaniko
  labels:
    app : kaniko
spec:
  containers:
  - name: kaniko
    image: gcr.io/kaniko-project/executor:v1.9.0-debug
    tty: true
    imagePullPolicy: IfNotPresent
    command:
    - /bin/sh
            """,
            yamlMergeStrategy: jenkins.merge(),
            workspaceVolume: jenkins.persistentVolumeClaimWorkspaceVolume(
                claimName: "pvc-workspace-${jenkins.env.JENKINS_AGENT_NAME}",
                readOnly: false
            )
        )
        {
            jenkins.node(jenkins.POD_LABEL){
                jenkins.container('kaniko') {
                    jenkins.echo "Build and Publish Docker image Step"
                    jenkins.sh label: "Build image with Kaniko", script: """"
                        /kaniko/executor -f `pwd`/Dockerfile -c `pwd` --insecure --skip-tls-verify --cache=true --no-push --destination=\${DOCKER_IMAGE}:\${APP_VERSION}.\${GIT_COMMIT}
                    """
                }
            }
        }
    }
}