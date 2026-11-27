import sys,zipfile,struct
with zipfile.ZipFile(sys.argv[1]) as z:
    libs=[n for n in z.namelist() if n.startswith('lib/') and n.endswith('.so')]
    arm=[n for n in libs if n.startswith('lib/arm64-v8a/')]
    assert arm, 'Missing arm64-v8a native libraries'
    for n in arm:
        b=z.read(n)
        assert b[:4]==b'\x7fELF' and b[4]==2 and b[5]==1,n
        machine=struct.unpack_from('<H',b,18)[0]
        assert machine==183,(n,machine)
        off=struct.unpack_from('<Q',b,32)[0]
        size,count=struct.unpack_from('<HH',b,54)
        for i in range(count):
            typ,flags,offset,vaddr,paddr,filesz,memsz,align=struct.unpack_from('<IIQQQQQQ',b,off+i*size)
            if typ==1: assert align>=16384 and (vaddr-offset)%16384==0,(n,'ELF alignment',align)
        print(n,'ARM64 16KB ELF verified')
