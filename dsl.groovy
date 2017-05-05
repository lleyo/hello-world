import java.util.List;
import com.xebialabs.xlrelease.domain.variables.Variable;

/* 
README 
In XLR must be configured the following parameters
	Jenkins server with the following properties
		Type: jenkins.Server
		Title: The same value as the variable jenkinsServerTitle in this DSL (i.e: Jenkins orquestacion)

	Global vars:
		In order to access to jenkins, it need to have defined de password of the jenkins user
			The global var must be: global.jenkinsPass (type password), and the user will be the user defined in jenkins.Server in shared configuration.
			It's mandatory to config global.jenkinsPass because the jenkins.Server.userpass cannot be obtained in this DSL (it returns null)
			
	Variables in the parent template
		It's necessary to create the following vars in the template witch will invoke the DSL
			releaseName
			releaseVersion
		Both of them are required and must be asked at the begining of the release, because they are used to set the title of the DSL release
*/

/* Title of the share configuration for Jenkins */
def jenkinsServerTitle = "Jenkins orquestacion"

/* Path to the java project that manages the dependencies*/
def pathRestAPIJar = "C:/proyectos/XLRelease/gitXlRelease/restAPI/target/restAPI-0.1.0-SNAPSHOT.jar"

/* method to obtain a shared configuration */
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

def xlJenkinsServer = server('jenkins.Server',jenkinsServerTitle)

/* var with the jenkins's pass */
def jenkinsPass

List<Variable> globalVars = configurationApi.getGlobalVariables()
for (Variable var : globalVars) {
	print var.getKey();
	if ("global.jenkinsPass".equals(var.getKey())) {
		jenkinsPass = var.getValue();
	}
}

/* Python script to execute the Java program for managing dependencies */
def dependenciesScript = "" +
"import subprocess\n" +
"from subprocess import CalledProcessError, check_output\n" +
"try:\n" +
"   output = subprocess.check_output(['java', '-jar', '" + pathRestAPIJar + "', '\$"+"{release.id}'])\n" +
"except CalledProcessError as e:    \n" +
"    output = e.output\n" +
"    print(output)\n" +
"    returncode = e.returncode\n" +
"    print(returncode)\n" +
"    sys.exit(1)\n" +
"print(output)\n"

/* Defining release and version name */
def releaseName
def releaseVersion
List<Variable> vars = releaseApi.getVariables(String.valueOf("${release.id}"))
for (Variable var : vars) {
	if ("releaseName".equals(var.getKey())) {
		releaseName = var.getValue();
	} else if ("releaseVersion".equals(var.getKey())) {
		releaseVersion = var.getValue();
	}
}

/* DSL */
createdRelease = xlr {
    release(releaseName + " " + releaseVersion) {
        description "Hello world release description"
		variables {
			stringVariable("statusPlanResult")
			stringVariable("releaseCreatedId")
		}	
        phases {
            phase {
                title "Plan"
                tasks {
                    script {
						title "Invoke Java dependencies program"
						description "Invoke Java dependencies program"
						script dependenciesScript
					}
					custom('Phase status Plan') {
						description 'Get Jenkins pipe line status. If it is completed continue'
						script {
							type 'jenkins.CheckPipelineStatusPhase'
							jenkinsServer xlJenkinsServer
							jobId 'BBVA_Global_Orchestration/job/apx-test/job/develop'
							username xlJenkinsServer.username
							password jenkinsPass
							phaseName 'Plan'
							statusResult variable('statusPlanResult')
						}
					}
                }
            }
			phase {
                title "Build"
                tasks {                   
					custom('Phase status Build') {
						description 'Get Jenkins pipe line status. If it is completed continue'
						script {
							type 'jenkins.CheckPipelineStatusPhase'
							jenkinsServer xlJenkinsServer
							jobId 'BBVA_Global_Orchestration/job/apx-test/job/develop'
							username xlJenkinsServer.username
							password jenkinsPass
							phaseName 'Build'
							statusResult variable('statusPlanResult')
						}
					}
                }
            }
			phase {
                title "Unit Test"
                tasks {                   
					custom('Phase status Unit Test') {
						description 'Get Jenkins pipe line status. If it is completed continue'
						script {
							type 'jenkins.CheckPipelineStatusPhase'
							jenkinsServer xlJenkinsServer
							jobId 'BBVA_Global_Orchestration/job/apx-test/job/develop'
							username xlJenkinsServer.username
							password jenkinsPass
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
