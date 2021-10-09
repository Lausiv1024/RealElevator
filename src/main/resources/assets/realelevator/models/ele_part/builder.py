from time import sleep

#後々使いやすいように処理する

li = ''
filename = 'variants.txt'
count = 0
linea = ''

with open('variants.txt') as f:
    while True:
        linea = f.readline()
        
        if not linea:
            break

        if linea[0] != '#':
            if count % 2 != 0 :
                li = li + 'minecraft:block/' +linea
            else:
                li = li + linea
            count = count + 1
        else:
            li = li + linea
            count = 0

print(li)

with open(filename, mode='w') as f1:
    f1.write(li)