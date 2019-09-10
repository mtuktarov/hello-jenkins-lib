#!/usr/bin/env groovy
package devops
class Constants {
    static final COLOR_GRAY = '#D4DADF'
    static final COLOR_GREEN = '#BDFFC3'
    static final COLOR_YELLOW = '#FFFE89'
    static final COLOR_RED = '#FF9FA1'
    static final PIPELINE_STARTED = 'STARTED'
    static final PIPELINE_SUCCESS = 'SUCCESS'
    static final PIPELINE_UNSTABLE = 'UNSTABLE'
    static final PIPELINE_FAILURE = 'FAILURE'
    static final PIPELINE_ABORTED = 'ABORTED'

    static DOCKER_REGISTRY_CREDS_ID = 'mtuktarov-docker-hub'
    static final DOCKER_BUILD_PARAMS = '-f Dockerfile . --no-cache'
