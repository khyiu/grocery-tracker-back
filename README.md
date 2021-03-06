# expense-tracker-back
This is a personal project that I'll use to get familiar with tools and techniques I've wanted to explore for a while.  

SonarCloud status: [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=khyiu_expense-tracker-back&metric=alert_status)](https://sonarcloud.io/dashboard?id=khyiu_expense-tracker-back)

# Journey log
## Stage 1: CI/CD server
### Circle CI
Initially I gave Circle CI a shot. As many other CI solutions, it was pretty straightforward to define a first working 
build pipeline: based on a yaml configuration and some commands I've found in some examples, I managed to have my project
build running. 

Right after, I wanted my build pipeline to push the build artifact to AWS CodeArtifact where I created a Maven repository.   
I browsed the Orbs registry but couldn't find any dedicated to AWS CodeArtifactory. I could have tried using the orb
that installs AWS CLI and try to run the proper command to do so but I figured, why not switch the CI/CD process 
altogether to AWS.

### AWS
#### CodeBuild

Project build process. Fairly easy and intuitive initial setup: simply created a project in the AWS CodeBuild console 
and specify to use the _buildspec.yml_ configuration file from the project source.
Didn't even bother to configure input/output folders in the dedicated S3 bucket -> use default values from CodeBuild.

#### ElasticBeanStalk
Serving of web application. Creating a basic environment for my Spring boot application was really easy. I've only had 
to select Java environment, the version of Java to use, and specify the S3 URL where my application artifact would be 
pushed to.

#### CodePipeline
Automation of release process. Once again, initial setup was not too difficult.  
My pipeline is quite simple as well: 

1. Source checkout from Github
1. Build project based on CodeBuild configuration
1. Push build artifact to the S3 bucket that is used by Elastic BeanStalk

#### IAM
During the first few executions of my pipeline, I often ran into permission issues. For instance, the "CodeBuild" role 
didn't have permission to list objects, push objects, ... to a certain S3 bucket.  
To fix this kind of issue, I figured the simplest way is to:
1. open the IAM console
1. select the concerned role
1. open the security policy attached to this role
1. edit the security policy to add the required permission, and specify the resource to which it applies, if necessary

## Stage 2: Automated code review/coverage
### Sonarcloud
To get code coverage metrics and automated code review, I've decided to have my project scanned by Sonarcloud.  
The setup was super easy. You only need to authorize Sonarcloud to get access to your Github account (because I'm using 
Github as my SCM), and select the project you want to be analyzed.  
To trigger a Sonarcloud analysis every time my project is built, I had to:
* add a couple of properties to my _pom.xml_ to idenfity my Sonar project  
`		<sonar.projectKey>khyiu_expense-tracker-back</sonar.projectKey>
 		<sonar.organization>khyiu</sonar.organization>
 		<sonar.host.url>https://sonarcloud.io</sonar.host.url>`  
 		Those could be copied from Sonarcloud -> super user friendly
* in _AWS SystemS Manager_, I've created a parameter of type _secured string_ in the _parameter store_ to hold the login 
to my Sonarcloud instance. I've called this parameter SONAR_LOGIN and it can be read from my _buildspec.yml_ 
configuration
* in the build phase of my _buildspec.yml_, I've added the following Maven command to actually trigger the analysis:
`mvn sonar:sonar -Dsonar.login=$SONAR_LOGIN`	
* :exclamation: For some reason, the Maven Sonar plugin only works if the project to analyse is in a Git work tree. 
The problem is that my AWS pipeline is only fetching the raw sources from my Github repository. As a (temporary ?) hack, 
I've added a `git init` command prior to triggering the Sonarcloud analysis, and the Maven plugin stopped complaining ...

## Stage 3: IAM
### Okta
With all the marketing people at Okta have done, I've decided to check out their website and what they have to offer.
As they offer a free developer edition hosted on their cloud, let's give it a go!

Following the guides Okta provides, I managed to integrate user authentication through Okta. 
But the free developer edition is lacking RBAC. In the meantime, I've discovered AWS Cognito, and figured I could switch
to it for a try.
 
### AWS Cognito
To integrate AWS Cognito, I've followed this [tutorial](https://kevcodez.de/posts/2020-03-26-getting-started-aws-cognito).

:exclamation: Cognito only supports HTTPS login callback URLs. So, I first need to enable HTTPS for my Elastic BeanStalk 
single instance.

#### Enabling HTTPS for Elastic BeanStalk single instance
As I'm deploying my Spring-Boot app packaged as a jar, I followed this official doc from AWS: 
[Terminating HTTPS on EC2 instances running Java SE](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/https-singleinstance-java.html).

I tried a whole day to make it work but yet couldn't.  
It turns out the current documentation only applies to EB instances running on Amazon Linux 1. For more details, take a 
at this [Stackoverflow post](https://stackoverflow.com/questions/62986216/aws-elastic-beanstalk-single-instance-how-to-enable-https-for-a-spring-boot).

Eventually, I switched my app to new EB environment that is using Java 8 on Amazon Linux 1 and finally got my app to 
support HTTPS.

## Stage 4: IAM using AWS Cognito
1. create a Cognito user pool dedicated to Expense tracker app (:exclamation: don't forget that Cognito only support HTTPS 
callback URL -> the application has been configured with SSL enabled)
1. Define a Spring configuration class that enables web security:
    - `OAuth2SecurityConfig` extending `WebSecurityConfigurerAdapter` and annotated with `@EnableWebSecurity`
    - Override the `configure(HttpSecurity http)` method as follows, to configure authentication using OAuth2 : 
    ```java
    http.authorizeRequests()
       .oauth2Login()   
       ...
    ````
   - provide a Bean of type `ClientRegistrationRepository` through which we register the OAuth client for AWS Cognito
   
## Stage 5: AWS DynamoDB
1. Create `Purchase` table in AWS DynamoDB
1. Follow instructions from [AWS DynamoDB documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DAX.client.create-user-policy.html)  
to create user policy, access and secret keys to connect to the table created in previous step
1. To integrate DynamoDB in the app, use this [tutorial](https://www.baeldung.com/spring-data-dynamodb) as a base
1. For integration tests, as obviously, we don't want to expose access and secret keys in the code base:
    - create a variable in AWS property store for endpoint, access and secrey keys
    - edit user policy associated to `codebuild` to grant it read access to those properties
    - edit `buildspec.yml` file to load those properties and pass them as Maven system properties, so that Spring is able to inject them using `@Value`
1. Use `NoSQL Workbench for Amazon DynamoDB` tool to design table and synchronize it to AWS  
