#blockstateもpythonに作らせる
import sys

start_hinagata = '''
{
    "variants":{
'''
end_hinagata = '''
    }
}
'''

varType = ''

variant_name = []
lis = []

if len(sys.argv) == 1:
    print('Bad Arguments!')
    sys.exit()

#Dataファイルの読み込み
with open('variants.txt', mode='r') as variantF:
    i = 0
    while True:
        a = variantF.readline()
        if not a:
            break
        if a[0] == '#':
            varType = a.replace('#', '').rstrip()
            i = 0
        else:
            variant_name.append('       "type=' + a.rstrip() + '" : {"model" : "realelevator:ele_part/door/nowindow/h4/' + varType + '/door_no_window24_' + a.rstrip() + '"},\n')

        i = i + 1

asd = variant_name[len(variant_name) - 1]
variant_name[len(variant_name) - 1] = asd.strip(",")
a = sys.argv[1]
bb = a.replace('.json', '')
cc = bb.split('_')

#BlockstateのModelsの部分を作る
# for i in range(len(variant_name)):
#     namee = variant_name[i]
#     #"type=<variantType>" : {"model" : <ModelName>}
#     hinagata = '        "type=' + namee.rstrip() + '" : {"model" : "realelevator:ele_part/door/nowindow/h3/door_no_window24_' + namee.rstrip() + '"},\n'
#     lis.append(hinagata)
    

with open(sys.argv[1], mode='w') as f:
    f.write(start_hinagata)
    f.writelines(variant_name)
    f.write(end_hinagata)
