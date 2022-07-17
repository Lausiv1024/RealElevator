#Jamb model creator

templateList = ["jamb_1_", "jamb_2_", "jamb_1_roof_", "jamb_2_roof_"]

def createFile(data):
    #parsing list
    datas = data.rstrip().split(',')
    nameTemplate = datas[0]
    texName = datas[1]
    #creating file
    for name in templateList:
        parent = name + "blockface"
        fName = name + nameTemplate + ".json"
        replaced = template1.replace('$parent', parent).replace('$face', texName)
        with open(fName, mode='w') as fw:
            fw.write(replaced)


with open("template.json") as ft:#load template file
    template1 = ft.read()

with open("list.txt") as fl:
    while True:
        data = fl.readline()
        if not data:
            break
        createFile(data)