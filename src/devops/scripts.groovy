#!/usr/bin/env groovy
package devops

def getActions(){
    String actionsScript = """
        return ['Run tests', 'Build image', 'Build and Deploy image', 'Deploy an image']
    """
    return actionsScript
}

def getImports(){
    return """
        import com.cloudbees.plugins.credentials.Credentials
        import jenkins.model.Jenkins
        import groovy.json.JsonSlurper

        """
}
def getBranchesOrTags(String imageName, String dockerRegistryCreds, String repoUrl){

    String branchesOrTagsScript = """
        ${getImports()}
        if (binding.variables.get('action').contains('Build') || binding.variables.get('action').contains('Run')) {
            ${getBranches(repoUrl)}
        }
        else if (binding.variables.get('action').contains('Deploy')){
            ${getTags(imageName, dockerRegistryCreds, false)}
        }
    """
    return branchesOrTagsScript
}

def getTags(String imageName, String dockerRegistryCreds, boolean withImports=true){

    String tagsScript = (withImports) ? getImports() : ''

    tagsScript +="""

        credId = "${dockerRegistryCreds}"

        Set<Credentials> allCredentials = new HashSet<Credentials>();

        def creds = com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(
              com.cloudbees.plugins.credentials.Credentials.class
        );

        allCredentials.addAll(creds)
        def authString
        if(allCredentials != null){
            for (def cred in allCredentials){
                if ( cred.id == credId ) {
                    def token_response = ["curl", "-s", "-H", "Content-Type: application/json", "-d", "{\\"username\\": \\"\${cred.username}\\", \\"password\\": \\"\${cred.password}\\"}", "https://hub.docker.com/v2/users/login/"].execute().text.replaceAll("\\r\\n", "")
                    def token_data = new groovy.json.JsonSlurperClassic().parseText(token_response)
                  authToken = token_data.token
                }
            }
        }

        def responseTags = ["curl", "-H", "Authorization: JWT \${authToken}", "-k", "-X", "GET", "https://hub.docker.com/v2/repositories/mtuktarov/hello/tags"].execute().text.replaceAll("\\r\\n", "")
        def dataTags = new groovy.json.JsonSlurperClassic().parseText(responseTags)


        def tagsByDate = [:]
        def timestamps = []
        def sortedTags = []

        dataTags.results.each{
            tagsByDate[it.last_updated] = it.name
            timestamps.push(it.last_updated)
        }

        timestamps = timestamps.sort().reverse()

        for(timestamp in timestamps){
            sortedTags.push(tagsByDate[timestamp])
        }

        return sortedTags
    """
    return tagsScript
}

def getBranches(String repoUrl){

    String branchesScript = """
        def gettags = ("git ls-remote -t -h ${repoUrl}").execute()

        return gettags.text.readLines().collect {
            it.split()[1].replaceAll(\'refs/heads/\', \'\').replaceAll(\'refs/tags/\', \'\').replaceAll("\\\\^\\\\{\\\\}", \'\')
        }
    """
    return branchesScript
}
