# Introduction

Hello. In this directory you'll find two projects with git repos. `webdir-spa` is an Angular SPA that communicates with `webdir-api`, the API built on Pedestal using the Jetty web server. 

The SPA is served as a static resource by the Jetty server in the Docker image, but they could easily be decoupled. The server API is stateless and uses an in-memory database that's seeded from [randomuser.me](https://randomuser.me) at startup (with a seed for consistency), but could easily be hooked up to any number of persistence layers. 

I chose these technologies because they were a mix of things I know and things I don't. I wanted the project to be an opportunity to explore some new technologies and learn things I've been meaning to explore, but haven't had a direct and time-limited reason to dive into until now.

This was fun! I learned a lot. And I'm excited to have a conversation about it all. I'm sure I went over the recommended time, but I'm not sure how much. I read about a lot of new technologies and libraries, and tried some things out that didn't pan out and pivoted. 

# To run locally through Docker

I know people who do not do Java dev despise the JDK. So!

1. Edit the `Dockerfile` to pull the correct OpenJDK image for your CPU's architecture.
2. Build the image with `docker build -t webdir-api .` The Uberjar is pre-built.
3. Run the docker image with: `docker run -p 8080:8080 webdir-api`
4. In your browser, navigate to [http://localhost:8080](http://localhost:8080) to see the SPA. You can also view the Swagger UI at [http://localhost:8080/api/](http://localhost:8080/api/)


# Build instructions

Assuming you have [Clojure](https://clojure.org/guides/getting_started) and the [Angular CLI](https://angular.io/cli) installed you can build SPA and the Uberjar like so:

1. Build the SPA in the `web-spa` directory with `npm install && ng build`
2. Build the Uberjar in the `web-api` directory with `clojure -T:build org.corfield.build/uber :lib net.jtth/webdir-api :main webdir-api.server`
3. Build the Dockerfile in the `web-api` directory with `docker build -t webdir-api .`
4. Run the docker image above.

# Commentary

I know this project was meant to start a conversation, so what follows is me having some of that conversation with myself for practice.

My normal development process is to create feature or bug branches from JIRA tickets or GitHub issues, then merge PRs into a main branch hooked up to CI/CD. I didn't do this for speed's sake here, but would on any non-rush project, and will in the future no matter what the constraints were. Whatever the overhead costs in time would be made up in sanity and a systematic commit log. 

## The SPA

I use Angular for my main project, iReadBetter, but have never written custom router animations or used TailwindCSS at all. Angular solves a lot of the decisions when quickly building an SPA because it includes most of the functionality a complex app would require, so I didn't have to select libraries for each piece. I used templates from TailwindUI to get quick and pleasing layouts and elements, and use the Angular DataTables component to get client-side pagination, search, and column sorting. Unless there's many thousands of employees, it makes more sense to serve the entire employee list once in a while and filter or sort it client-side. If server-side search pagination were required for a very large client then both the DataTable implementation and API could be updated pretty easily. (DataTables can be directly hooked up to a paginated remote API.) One place where Angular really shines is with forms and form validation, though I could've done more with that given more time. My main takeaway is that Tailwind is fantastic for prototyping but makes for some busy HTML templates with huge `class` attributes. Tradeoffs abound.

What I would do differently if I had more time:

- Lots more tests, and any e2e tests. In my main consulting project, iReadBetter, I discussed test-driven development with the PI and explained to him how it would make catching regressions automatic and let us define behavior in a declarative way, but that it would take quite a bit of time because of the rich interactive nature of the app. I presented this to him as a tradeoff between rapid-but-messy development and prototyping and the more systematic but slower cycles of iteration with testing. In retrospect, I wouldn't give any future clients the option. But I lack a lot of experience implementing tests in general, specifically with e2e tests. This is the next thing on my professional development todo list.
- The animations are a little choppy, but I wanted to include something to show how I would do it.
- I did not add a "net" service for brevity's sake. If I had the time, I would have it do back off timeouts and all the good net citizen stuff, and expose its data through BehaviorSubjects or some other reactive data structure. Right now it simply retries three times and throws an error if something goes wrong in fetching or uploading individual employee data.
- I would add drag and drop upload for images, and/or support and forms for multiple image sizes as provided by the Random People API.
- I would add a more styled search bar, hide the DataTables one, and rebroadcast the pretty one into the DataTables search input to make things look nice and balanced.

## The API

I wrote the API in Clojure using the [Pedestal](http://pedestal.io/) suite of libraries for making APIs. I love Clojure and relish opportunities to write in it. Clojure can be hosted in many languages (JavaScript, .Net CLR), but is most popular on Java, where Pedestal, a set of libraries for robust API development, was meant to live. I picked Pedestal as it is a well respected basis for API development on Clojure and developed by the company that created and shepherds Clojure. I had initially set out to try to build on Wal-Mart's `lacinia-pedestal`, but decided against it as I have not used GraphQL before. One other alternative was to take the Person data and put it into a PostgreSQL database and use something like Hasura or Supabase's `pg_graphql` to wrap schema around the relational structure in Postgres. But since I've never used GraphQL before, I figured I shouldn't do it in a time crunch. 

Because there was no requirement for persistence I am just using a Clojure atom as the "database." This is basically a map where `employee-id` is the key and the map representing the employee is the value (including the id). This also makes things like search pretty straightforward to implement. In production, for something at scale, I'd use Datomic (or XTDB) to ensure persistent and durable data in a way that is implementation-agnostic and affords composable queries through Datalog and SQL. Both can use Postgres, Aurora, DynamoDB, or other databases as their own data stores in many distributed configurations in a way the backend doesn't need to know about. (XTDB also has an experimental GraphQL API that supposedly works kind of like Hasura. I actually wrote a Datomic schema for the employee data, but stopped implementing it when I realized Datomic doesn't have a freely redistributable version anymore.)

Other things I would do differently if I had more time:

- Again, I would write more tests. Pedestal in particular has a great template for this; it's not a process I'm used to and I was going for feature maximalism in this project. I would follow TDD without the time pressure. 
- I used [Pedestal-api](https://github.com/oliyh/pedestal-api) to get some nice wrappers for building a REST API quickly with a Swagger front end and built-in schema validation. If I were not under a time pressure I would use Pedestal directly without Pedestal-api as it does not add that much when tools like Postman exist, and got in the way a few times as it constructs things a little differently than the Pedestal documentation describes, which slowed me down in a few places.
- I would decouple all the business logic from the request handlers.
- I would handle validations with Clojure's spec library rather than Plumatic's Schema, which is built into Pedestal-api.
