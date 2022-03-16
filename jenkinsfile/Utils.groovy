
//--- global variables ---
host = "https://github.com/jprubianes"
repo = "acquire"
containers = []
pythonImg = null
workspace = null
currentJob = null
currentBranch = null
currentTag = null
currentNetwork = null
seleniumVersion = "3.141.59"


//--- common utility methods ---
def setup(env) {
    echo "--- Setup ---"
    workspace = pwd()
    currentJob = env.JOB_NAME.replace("/" + env.JOB_BASE_NAME, "").replaceAll(/[^a-zA-Z0-9]/, "_")
    currentTag = env.BRANCH_NAME.replaceAll(/[^a-zA-Z0-9]/, "_")
    currentBranch = env.BRANCH_NAME
    currentNetwork = currentJob + "_" + currentTag
    currentNetwork = currentNetwork.toLowerCase()
    this.createNetwork currentNetwork

    pythonImg = this.dockerImage name: "python", dockerfile: "jenkins/docker/python"
}

def teardown() {
    echo "--- Teardown ---"
    echo "Number of containers to destroy: " + ((List) containers).size()
    def removeList = [:]
    containers.each { container ->
        if (container) {
            removeList["Remove " + "${container.id}".take(5)] = {
                this.removeContainer container
            }
        }
    }
    parallel removeList
    this.removeNetwork currentNetwork
}


def checkoutRepo(host, repo, branch="master", credentials=null) {
    def url = host + "/" + repo
    dir(repo) {
        try {
            git url: url, branch: branch, poll: false
        }
        catch (error) {
            echo error.getMessage()
            git url: url, branch: "master", poll: false
        }
    }
}

def downloadArtifacts(jobName, branchName, filter, target) {
    def escapedBranchName = URLEncoder.encode(branchName, "UTF-8")
    try {
        copyArtifacts projectName: "../${jobName}/${escapedBranchName}", selector: lastSuccessful(), filter: filter, flatten: true, target: target
    } catch (error) {
        echo "Build not found for \"${jobName}\" project -> \"${branchName}\" branch, so using master build instead..."
        copyArtifacts projectName: "../${jobName}/master", selector: lastSuccessful(), filter: filter, flatten: true, target: target
    }
}

def replaceInFile(file, replace, with) {
    sh "sed -i -- 's|${replace}|${with}|g' ${file}"
}

def dockerImage(Map args) {
    def imageTag = currentJob.toLowerCase() + "_" + args.name + ":" + currentTag.toLowerCase()
    return docker.build(imageTag, args.dockerfile)
}

def imageFromDockerfile(job, tag, shortName, dockerfile) {
    def imageTag = "${job}_${shortName}_image:${tag}"
    return docker.build(imageTag, dockerfile)
}

def removeContainer(container) {
    if (container == null) return
    try {
        sh "docker stop ${container.id} || echo Unable to stop container ${container.id}"
        sh "docker rm -f ${container.id} || echo Unable to remove container ${container.id}"
    } catch (error) {
        echo "Error while removing a container: " + error.getMessage()
    }
}

def createNetwork(network) {
    removeNetwork network
    sh "docker network create ${network}"
}

def removeNetwork(network) {
    try {
        sh "docker network rm ${network}"
    } catch (error) {
        echo "unable to remove network"
        sh "docker inspect ${network} || true"
    }
}

def connectNetwork(network, container, alias) {
    sh "docker network connect --alias ${alias} ${network} ${container.id}"
}

def dockerExec(container, script) {
    sh "docker exec -i ${container.id} ${script}"
}

def dockerLogs(container, output = null) {
    try {
        if (container != null) {
            if (output != null) {
                sh "docker logs ${container.id} > ${output}"
            } else {
                sh "docker logs ${container.id}"
            }
        }
    } catch (error) {
        echo "Error getting logs from container: " + error.getMessage()
    }
}

def dockerLogsFromFile(Map args) {
    try {
        if (args.container) this.dockerExec args.container, "bash -c 'cat ${args.file} > ${args.output}'"
    } catch (error) {
        echo "Error getting logs from file in container: " + error.getMessage()
    }
}

def dockerParams(Map map) {

    def workspace = pwd()
    def volumes = map.volumes
    def links = map.links
    def env = map.env
    def network = map.network
    def host = map.host

    if (volumes == null) volumes = []
    if (links == null) links = []
    if (env == null) env = []

    def params = " --privileged -u root "
    params += " -v ${workspace}/.m2:/root/.m2:rw "
    params += " -v ${workspace}/.ivy2:/root/.ivy2:rw "
    params += " -v ${workspace}/.gradle:/root/.gradle:rw "

    volumes.each{ dir, mount ->
        params += " -v ${dir}:${mount}:rw "
    }

    links.each{ hostname, container ->
        if (container != null) {
            params += " --link ${container.id}:${hostname} "
        } else {
            echo "WARNING: linking was skipped for ${hostname}, container was null"
        }
    }

    env.each{ key, value ->
        params += " -e ${key}=${value} "
    }

    if (network) {
        params += " --net=${network} "
        if (host) {
            params += " --net-alias=${host} "
        }
    }

    return params
}

def startSeleniumGrid(Map args) {
    def chrome = args.chrome
    def sessionPerNode = args.sessionPerNode

    def hubImg = docker.image "selenium/hub:$seleniumVersion"
    def chromeImg = docker.image "selenium/node-chrome:$seleniumVersion"

    if (!chrome) chrome = 0
    if (!sessionPerNode) sessionPerNode = 3

    echo "Starting Selenium Hub"
    def hubParams = this.dockerParams(
            network: currentNetwork,
            host: "selenium",
            env: [
                    "GRID_MAX_SESSION": (chrome) * sessionPerNode
            ],
            volumes: [
                    "/dev/shm": "/dev/shm"
            ]
    )
    hub = hubImg.run(hubParams)
    containers << hub

    echo "Starting Selenium Nodes"
    def nodeParams = this.dockerParams(
            network: currentNetwork,
            env: [
                    "HUB_HOST": "selenium",
                    "NODE_MAX_INSTANCES": sessionPerNode,
                    "NODE_MAX_SESSION": sessionPerNode,
                    "SCREEN_WIDTH": 1600,
                    "SCREEN_HEIGHT": 1200
            ],
            volumes: [
                    "/dev/shm": "/dev/shm"
            ]
    )
    for (int i = 1; i <= chrome; i++) {
        echo "--> Chrome Node #${i}"
        containers << chromeImg.run(nodeParams)
    }
}

def runTest(Map args) {
    def buildParams = this.dockerParams(
            network: currentNetwork,
            env: [
                    "WORKSPACE_URL": "${env.BUILD_URL}/execution/node/3/ws"
            ],
            volumes: [
                    "${workspace}": "/source"
            ]
    )

    def healthcheckScript = ""

    if (args.healthcheck) {
        args.healthcheck.each { endpoint ->
            healthcheckScript += "\ncurl -X GET --retry 6 --retry-connrefused ${endpoint} -vsS\n"
        }
    }

    def testScript = healthcheckScript + args.script

    def test = args.image.inside(buildParams) {sh testScript}
    containers << test
    return test
}

def offlineReports(list) {
    try {
        def buildParams = this.dockerParams(volumes: ["${workspace}": "/source"])
        def python = pythonImg.inside(buildParams) {
            list.each { report ->
                sh "python /source/scripts/convert-html-to-offline.py /source/${report}"
            }
        }
        this.removeContainer python
    } catch (error) {
        echo "Error while converting offline reports: " + error.getMessage()
    }
}

// this line is very important otherwise Jenkinsfile will load null utils
return this
