vr = []

with open('variants.txt') as f1:
    i = 0
    while True:
        a = f1.readline()
        if not a :
            break
        
        if a[0] == "#":
            vr.append(a)
            i = 0
        elif i % 2 != 0:
            vr.append(a)

        i = i + 1

with open('variants.txt', mode='w') as f2:
    f2.writelines(vr)