# myapp

### 1. Build ClojureScript external dependencies

docker run -it -v `pwd`:/here kkarczmarczyk/node-yarn sh
cd /here/react-libs
yarn add webpack webpack-cli
yarn webpack

### 2. Start env

1. start flow (branch poc/survey-editor-alone):

    docker-compose -f docker-compose.together.yml up --build -d
    
1. start grouping things app:
     
    docker-compose up --build -d

1. Connect to the REPL (7888) 
1. Run the commented block at myapp.gae.core to import the flow data
1. Start figwheel `(start-fw)`
1. Connect to the figwheel repl (7889) and start the CLJS repl `(cljs)`
