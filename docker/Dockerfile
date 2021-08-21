FROM hseeberger/scala-sbt:11.0.12_1.5.5_2.13.6

# Set up user workspace
USER sbtuser:sbtuser
RUN mkdir -p /home/sbtuser/prime-services
WORKDIR /home/sbtuser/prime-services

# Copy project sources and build files
COPY --chown=sbtuser:sbtuser src ./src
COPY --chown=sbtuser:sbtuser build.sbt ./
COPY --chown=sbtuser:sbtuser project/build.properties project/plugins.sbt ./project/

# Install dependencies and compile
RUN sbt update
RUN sbt compile

# Execution parameters
ENTRYPOINT ["sbt"]
CMD ["shell"]
