#!/usr/bin/env groovy
package devops

def getActions(){
    String actionsScript = """
        return ['Build', 'Build and Deploy', 'Deploy']
    """
    return actionsScript
}

def getImports(){
    return """
        import jenkins.model.Jenkins
        import groovy.json.JsonSlurper
        import com.cloudbees.hudson.plugins.folder.properties.FolderCredentialsProvider.FolderCredentialsProperty
        import com.cloudbees.hudson.plugins.folder.AbstractFolder
        import com.cloudbees.hudson.plugins.folder.Folder
        import com.cloudbees.plugins.credentials.impl.*
        import com.cloudbees.plugins.credentials.*
        import com.cloudbees.plugins.credentials.domains.*
        """
}
def getBranchesOrTags(String imageName, String dockerRegistryCreds, String repoUrl){

    String branchesOrTagsScript = """
        ${getImports()}
        if (binding.variables.get('action') == 'Deploy') {
            ${getTags(imageName, dockerRegistryCreds, false)}
        } else {
            ${getBranches(repoUrl)}
        }
    """
    return branchesOrTagsScript
}

def getTags(String imageName, String dockerRegistryCreds, boolean withImports=true){

    String tagsScript = (withImports) ? getImports() : ''

    tagsScript +="""

        credId = "${dockerRegistryCreds}"

        def authString = ""
        def credentialsStore =
        Jenkins.instance.getAllItems(Folder.class)
            .findAll{}
            .each{
                AbstractFolder<?> folderAbs = AbstractFolder.class.cast(it)
                FolderCredentialsProperty property = folderAbs.getProperties().get(FolderCredentialsProperty.class)
                if(property != null){
                    for (cred in property.getCredentials()){
                        if ( cred.id == credId ) {
                            authString  = "\${cred.username}:\${cred.password}"
                        }
                    }
                }
            }

        def response = ["curl", "-u", "\${authString}", "-k", "-X", "GET", "https://hub.docker.com/v2/repositories/${imageName}/tags"].execute().text.replaceAll("\\r\\n", "")
        def data = new groovy.json.JsonSlurperClassic().parseText(response)

        def tagsByDate = [:]
        def timestamps = []
        def sortedTags = []

        data.each{
            tagsByDate[it.created] = it.name
            timestamps.push(it.created)
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
