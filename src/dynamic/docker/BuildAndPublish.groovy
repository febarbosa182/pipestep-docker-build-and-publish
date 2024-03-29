package dynamic.publish_image

class PublishImage{
    def call (jenkins) {

        def builds = [:]
        def architecturesParam = jenkins.env.ARCHITECTURES
        def architectures = architecturesParam.split(",")
        for (architecture in architectures) {
            builds["Build ${architecture}"] = {
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
                          readinessProbe:
                            exec:
                              command: [sh, -c, "ls -S /var/run/docker.sock"]
                          command:
                          - sleep
                          args:
                          - 1d
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
                            jenkins.env.DOCKER_BUILDKIT = "1"
                            // WE WILL NOT PUSH SINCE IT'S A LOCAL STACK
                            jenkins.sh label: "Build image and publish multi architecture", script: """
                                docker buildx create --use
                                docker buildx build --platform ${architecture.trim()} -t \${DOCKER_IMAGE}:\${APP_VERSION}.\${GIT_COMMIT} .
                            """
                        }
                    }
                }
            }
        }
        jenkins.parallel builds

        // jenkins.podTemplate(
        //     yaml: """
        //       apiVersion: v1
        //       kind: Pod
        //       spec:
        //         volumes:
        //         - name: docker-socket
        //           emptyDir: {}
        //         containers:
        //         - name: docker
        //           image: docker:20.10
        //           readinessProbe:
        //             exec:
        //               command: [sh, -c, "ls -S /var/run/docker.sock"]
        //           command:
        //           - sleep
        //           args:
        //           - 1d
        //           volumeMounts:
        //           - name: docker-socket
        //             mountPath: /var/run
        //         - name: docker-daemon
        //           image: docker:20.10-dind
        //           securityContext:
        //             privileged: true
        //           volumeMounts:
        //           - name: docker-socket
        //             mountPath: /var/run
        //     """,
        //     yamlMergeStrategy: jenkins.merge(),
        //     workspaceVolume: jenkins.persistentVolumeClaimWorkspaceVolume(
        //         claimName: "pvc-workspace-${jenkins.env.JENKINS_AGENT_NAME}",
        //         readOnly: false
        //     )
        // )
        // {
        //     jenkins.node(jenkins.POD_LABEL){
        //         jenkins.container('docker') {
        //             jenkins.echo "Build and Publish Docker image Step"
        //             def packageJson = jenkins.readJSON file: 'package.json'
        //             jenkins.env.APP_VERSION = packageJson.version
        //             jenkins.env.DOCKER_BUILDKIT = "1"
        //             // WE WILL NOT PUSH SINCE IT'S A LOCAL STACK
        //             jenkins.sh label: "Build image and publish multi architecture", script: """
        //                 docker buildx create --use
        //                 docker buildx build --platform linux/amd64,linux/arm64,linux/arm/v7 -t \${DOCKER_IMAGE}:\${APP_VERSION}.\${GIT_COMMIT} .
        //             """
        //         }
        //     }
        // }
    }
}