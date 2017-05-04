def server(type, title) {
  def cis = configurationApi.searchByTypeAndTitle(type, title)
  if (cis.isEmpty()) {
    throw new RuntimeException("No CI found for the Type and Title")
  }
  if (cis.size() > 1) {
    throw new RuntimeException("More than one CI found for the Type and Title")
  }
  cis.get(0)
}
def xlJenkinsServer = server('jenkins.Server','Jenkins orquestacion')

def depsScript = "" +
"import subprocess\n" +
"from subprocess import CalledProcessError, check_output\n" +
"try:\n" +
"   output = subprocess.check_output(['java', '-jar', 'C:/proyectos/XLRelease/gitXlRelease/restAPI/target/restAPI-0.1.0-SNAPSHOT.jar', '\\${release.id}'])\n" + 
"except CalledProcessError as e:    \n" +
"    output = e.output\n" +
"    print(output)\n" +
"    returncode = e.returncode\n" +
"    print(returncode)\n" +
"    sys.exit(1)\n" +
"print(output)\n"

createdRelease = xlr {
    release("Hello world release") {
        description "Hello world release description"
		variables {
			stringVariable 'statusPlanResult'
			stringVariable 'releaseCreatedId'
		}	
        phases {
            phase {
                title "Plan"
                tasks {
                    script {
						title "Sample Script task"
						description "Dynamic script task"
						script depsScript
					}
					custom('Phase status') {
						description 'Get all done issues'
						script {
							type 'jenkins.CheckPipelineStatusPhase'
							jenkinsServer xlJenkinsServer
							jobId 'BBVA_Global_Orchestration/job/apx-test/job/develop'
							username 'ignacio.martin.santamaria.contractor'
							password 'Password123!'							
							phaseName 'Plan'
							statusResult variable('statusPlanResult')
						}
					}
                }
            }
			phase {
                title "Build"
                tasks {                   
					custom('Phase status') {
						description 'Get all done issues'
						script {
							type 'jenkins.CheckPipelineStatusPhase'
							jenkinsServer xlJenkinsServer
							jobId 'BBVA_Global_Orchestration/job/apx-test/job/develop'
							username 'ignacio.martin.santamaria.contractor'
							password 'Password123!'							
							phaseName 'Build'
							statusResult variable('statusPlanResult')
						}
					}
                }
            }
			phase {
                title "Unit Test"
                tasks {                   
					custom('Phase status') {
						description 'Get all done issues'
						script {
							type 'jenkins.CheckPipelineStatusPhase'
							jenkinsServer xlJenkinsServer
							jobId 'BBVA_Global_Orchestration/job/apx-test/job/develop'
							username 'ignacio.martin.santamaria.contractor'
							password 'Password123!'							
							phaseName 'Unit test'
							statusResult variable('statusPlanResult')
						}
					}
                }
            }
        }
    }
}
releaseApi.start(createdRelease.id)
