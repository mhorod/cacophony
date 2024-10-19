import os
import json

for root, dirs, files in os.walk("examples/incorrect"):
    for name in dirs:
        obj = { "examples": [] }
        for file in os.listdir(os.path.join(root, name)):
            if os.path.isfile(os.path.join(root, name, file)):
                obj["examples"].append({ "path" : file, "errors" : {"lexerErrors":False}  })

        if obj["examples"]:
            with open(os.path.join(root, name, "descriptions.json"), "w") as f:
                json.dump(obj, f, indent=2)


