# Feature Status

## Implemented Features
- The system should be able to run both on the company’s data centers but also locally by developers on their machines. One of the issues we have with our current CI/CD is that we cannot run workflows either in part or in full locally to assist with development and debugging.
- For the definition of pipelines
  - we want to have the same organization as other CI/CD, stages and jobs
  - all jobs and stages must be able to run inside Docker containers
  - we must be able to define multiple pipelines for a repository and the definitions should follow the DRY principle
  - jobs and stages must be named and we should be able to refer to these names in the CLI and in the description
  - we must be able to specify the order of stages
  - we must be able to specify the order of jobs within a stage
  - all jobs in a stage should be run in parallel unless we specify dependencies between jobs

- the CLI must be able to validate a configuration and provide helpful error messages
- [C1.Configuration files in a folder](https://neu-seattle.gitlab.io/asd/cs6510f24/CS6510-F24/main/project/requirements.html#_c1_configuration_files_in_a_folder)
- [C2.Each configuration file is independent](https://neu-seattle.gitlab.io/asd/cs6510f24/CS6510-F24/main/project/requirements.html#_c2_each_configuration_file_is_independent)
- [C3.There should be a global section in the configuration file](https://neu-seattle.gitlab.io/asd/cs6510f24/CS6510-F24/main/project/requirements.html#_c3_there_should_be_a_global_section_in_the_configuration_file)
- [C3.1.Jobs should be able to use or override global keys](https://neu-seattle.gitlab.io/asd/cs6510f24/CS6510-F24/main/project/requirements.html#_c3_1_jobs_should_be_able_to_use_or_override_global_keys)
- [C3.2.Pipeline name is unique for the repository](https://neu-seattle.gitlab.io/asd/cs6510f24/CS6510-F24/main/project/requirements.html#_c3_2_pipeline_name_is_unique_for_the_repository)
- [C4.Stages have a default but the configuration file can override](https://neu-seattle.gitlab.io/asd/cs6510f24/CS6510-F24/main/project/requirements.html#_c4_stages_have_a_default_but_the_configuration_file_can_override)
- [C5.1.Job Configurations have a name](https://neu-seattle.gitlab.io/asd/cs6510f24/CS6510-F24/main/project/requirements.html#_c5_1_job_configurations_have_a_name)
- [C5.2.Job Configurations define their stage](https://neu-seattle.gitlab.io/asd/cs6510f24/CS6510-F24/main/project/requirements.html#_c5_2_job_configurations_define_their_stage)
- [C5.3.Job Configurations define their Docker image](https://neu-seattle.gitlab.io/asd/cs6510f24/CS6510-F24/main/project/requirements.html#_c5_3_job_configurations_define_their_docker_image)
- [C5.4.Job Configurations define 1 or more commands](https://neu-seattle.gitlab.io/asd/cs6510f24/CS6510-F24/main/project/requirements.html#_c5_4_job_configurations_define_1_or_more_commands)
- [C5.6.Job Configuration can have dependencies](https://neu-seattle.gitlab.io/asd/cs6510f24/CS6510-F24/main/project/requirements.html#_c5_6_job_configuration_can_have_dependencies)
- [C5.6.1.Job dependencies cannot form cycles](https://neu-seattle.gitlab.io/asd/cs6510f24/CS6510-F24/main/project/requirements.html#_c5_6_1_job_dependencies_cannot_form_cycles)
- [L3.Error reporting](https://neu-seattle.gitlab.io/asd/cs6510f24/CS6510-F24/main/project/requirements.html#_l3_error_reporting)

Note: We added placeholder commands such as `log`, `rerun`, `stop`. These are pseudo-commands that have not been implemented yet.

## Partially Implemented Features
- [U1. Remote Repo Local CI/CD Run](https://neu-seattle.gitlab.io/asd/cs6510f24/CS6510-F24/main/project/requirements.html#_u1_remote_repo_local_cicd_run)
  - `--repo` and `--commit` are not implemented
- [U2. Local Repo Local CI/CD Run](https://neu-seattle.gitlab.io/asd/cs6510f24/CS6510-F24/main/project/requirements.html#_u2_local_repo_local_cicd_run)
  - `--repo` and `--commit` are not implemented
- [Running the pipeline](https://neu-seattle.gitlab.io/asd/cs6510f24/CS6510-F24/main/project/requirements.html#_l6_running_a_pipeline)
  - `--repo` and `--commit` are not implemented