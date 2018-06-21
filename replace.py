with open ("services.txt", "r") as myfile:
    services=myfile.read()
with open ("configs/docker-compose-cli.yaml", "r") as myfile:
    compose=myfile.read()
compose = compose.replace("__SERVICES__",services)
with open("configs/docker-compose-cli.yaml", "w") as text_file:
    text_file.write("%s" % compose)