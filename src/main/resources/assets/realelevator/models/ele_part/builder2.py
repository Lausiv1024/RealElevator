'''
エレベーターの部品の表面装飾用のリソース作成ツール
'''

import os
import sys

directryName = ''
srcFileName = ""

VARIANTFILENAME = "variants.txt"

TargetStr = 'tgt'

variantNames = []
texID = []
varTypes = []
varTypeIndex = []
count = 0

if len(sys.argv) == 1:
    print('[ERROR] NO Args')
    print('Useage : python builder2.py /dir [directryPath] /src [sourceFile]')
    sys.exit()

def CheckArgs():
    global directryName, srcFileName
    flagSourceFile = 0
    flagDirectry = 0
    for i in range(1, len(sys.argv)):
        if sys.argv[i] == '/src':
            flagSourceFile = i
        elif sys.argv[i] == '/dir':
            flagDirectry = i
        
        if flagSourceFile + 1 == i and flagSourceFile != 0:
            srcFileName = sys.argv[i]
        elif flagDirectry + 1 == i and flagDirectry != 0:
            directryName = sys.argv[i]

CheckArgs()

print ('Source: ' + srcFileName)
print('Directry :   ' + directryName)

if srcFileName == '' or directryName == '':
    print('[ERROR] Lack of Arguments.')
    print('Useage : python builder2.py /dir [directryPath] /src [sourceFile]')
    sys.exit()

with open(VARIANTFILENAME) as f:
    lNum = 0
    while True:
        ll = f.readline()
        if not ll:
            break

        if ll[0] == '#':
            varTypes.append(ll.strip('#').strip())
            varTypeIndex.append(lNum)
        
        lNum = lNum + 1

print(varTypes)
print(varTypeIndex)

for i3 in range(len(varTypes)):
    if not os.path.exists(directryName + '/' + varTypes[i3]):
        os.mkdir(directryName + '/' + varTypes[i3])

with open(VARIANTFILENAME) as variantF:
    cType = ''
    while True:
        lin = variantF.readline()
        if not lin:
            break
        #パイモン(非常食)とパイソン(Python)って、似てるよな？
        if lin[0] == '#':
            cType = lin.strip().strip('#')
            count = 0
        else:
            if count % 2 == 0:
                with open(srcFileName) as source:
                    srca = source.read()
                    moddedName = srcFileName.replace('.json', '') + "_" + lin.strip() + '.json'
            else:
                m = srca.replace(TargetStr, lin.strip())
                with open(directryName + '/' + cType + '/' + moddedName, mode='w') as newFile:
                    newFile.write(m)

            count = count + 1