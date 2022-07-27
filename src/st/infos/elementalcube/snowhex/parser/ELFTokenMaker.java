package st.infos.elementalcube.snowhex.parser;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import st.infos.elementalcube.snowhex.Token;
import st.infos.elementalcube.snowhex.Token.Level;
import st.infos.elementalcube.snowhex.TokenMaker;

public class ELFTokenMaker extends TokenMaker {

	private ByteOrder endianness;
	private int ptrsize;

	private static final String[] OS_ABIS = { "System V", "HP-UX", "NetBSD", "Linux", "GNU Hurd", null, "Solaris",
			"AIX (Monterey)", "IRIX", "FreeBSD", "Tru64", "Novell Modesto", "OpenBSD", "OpenVMS", "NonStop Kernel",
			"AROS", "FenixOS", "Nuxi CloudABI", "Stratus Technologies OpenVOS" };

	@Override
	public List<Token> generateTokens(byte[] array) {
		ArrayList<Token> list = new ArrayList<>();
		gen:
		try {
			if (array[0] != 0x7f || array[1] != 'E' || array[2] != 'L' || array[3] != 'F') {
				list.add(createToken(TOKEN_FILE_HEADER, 0, 4, invalidSignatureNotice(), Level.ERROR));
				break gen;
			}
			list.add(createToken(TOKEN_FILE_HEADER, 0, 4));
			ByteBuffer buf = ByteBuffer.wrap(array);
			buf.position(4);
			byte addrsize = buf.get();
			if (addrsize != 1 && addrsize != 2) {
				list.add(createToken(TOKEN_METADATA, buf.position() - 1, 1, notice("addrsize.error"), Level.ERROR));
				break gen;
			} else {
				ptrsize = addrsize == 2 ? 8 : 4;
				list.add(createToken(TOKEN_METADATA, buf.position() - 1, 1, notice("addrsize." + addrsize), Level.INFO));
			}
			byte bo = buf.get();
			if (bo != 1 && bo != 2) {
				list.add(createToken(TOKEN_METADATA, buf.position() - 1, 1, notice("endianness.error"), Level.ERROR));
				break gen;
			} else {
				endianness = bo == 2 ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
				buf.order(endianness);
				list.add(createToken(TOKEN_METADATA, buf.position() - 1, 1, notice("endianness." + bo), Level.INFO));
			}
			byte version = buf.get();
			if (version != 1) {
				list.add(createToken(TOKEN_METADATA, buf.position() - 1, 1, notice("version.error", version), Level.ERROR));
				break gen;
			} else {
				list.add(createToken(TOKEN_METADATA, buf.position() - 1, 1, notice("version", version), Level.INFO));
			}
			int osabi = buf.get() & 0xff;
			if (osabi > OS_ABIS.length || OS_ABIS[osabi] == null) {
				list.add(createToken(TOKEN_METADATA, buf.position() - 1, 1, notice("osabi.unknown", osabi), Level.ERROR));
			} else {
				list.add(createToken(TOKEN_METADATA, buf.position() - 1, 1, notice("osabi", OS_ABIS[osabi], osabi), Level.INFO));
			}
			list.add(createToken(TOKEN_METADATA, buf.position(), 1));
			list.add(createToken(TOKEN_RESERVED, buf.position() + 1, 7));
			buf.position(buf.position() + 8); // ABIVERSION + PADDING
			int type = buf.getShort() & 0xffff;
			if (type <= 4) {
				list.add(createToken(TOKEN_METADATA, buf.position() - 2, 2, notice("type." + type), Level.INFO));
			} else {
				list.add(createToken(TOKEN_METADATA, buf.position() - 2, 2, notice("type.unknown", type), Level.WARNING));
			}
			int machine = buf.getShort() & 0xffff;
			list.add(createToken(TOKEN_METADATA, buf.position() - 2, 2, notice("machine", machine), Level.INFO));
			int eversion = buf.getInt();
			if (eversion != 1) {
				list.add(createToken(TOKEN_METADATA, buf.position() - 4, 4, notice("version.error", eversion), Level.ERROR));
				break gen;
			} else {
				list.add(createToken(TOKEN_METADATA, buf.position() - 4, 4, notice("version", eversion), Level.INFO));
			}
			long entry = getAddress(buf);
			list.add(createToken(TOKEN_LENGTH, buf.position() - ptrsize, ptrsize, notice("entry", entry), Level.INFO));
			long phoff = getAddress(buf);
			list.add(createToken(TOKEN_LENGTH, buf.position() - ptrsize, ptrsize, notice("phoff", phoff), Level.INFO));
			long shoff = getAddress(buf);
			list.add(createToken(TOKEN_LENGTH, buf.position() - ptrsize, ptrsize, notice("shoff", shoff), Level.INFO));
			/* int flags = */ buf.getInt();
			list.add(createToken(TOKEN_METADATA, buf.position() - 4, 4));
			/* short ehsize = */ buf.getShort();
			list.add(createToken(TOKEN_LENGTH, buf.position() - 2, 2));
			int phentsize = buf.getShort() & 0xffff;
			list.add(createToken(TOKEN_LENGTH, buf.position() - 2, 2, notice("phentsize", phentsize), Level.INFO));
			int phnum = buf.getShort() & 0xffff;
			list.add(createToken(TOKEN_LENGTH, buf.position() - 2, 2, notice("phnum", phnum), Level.INFO));
			int shentsize = buf.getShort() & 0xffff;
			list.add(createToken(TOKEN_LENGTH, buf.position() - 2, 2, notice("shentsize", shentsize), Level.INFO));
			int shnum = buf.getShort() & 0xffff;
			list.add(createToken(TOKEN_LENGTH, buf.position() - 2, 2, notice("shnum", shnum), Level.INFO));
			int shstrndx = buf.getShort() & 0xffff;
			list.add(createToken(TOKEN_LENGTH, buf.position() - 2, 2, notice("shstrndx", shstrndx), Level.INFO));
			buf.position((int) phoff);
			// ph (Program Header) segments
			for (int segmentNumber = 0; segmentNumber < phnum; segmentNumber++) {
				int pType = buf.getInt();
				if (pType < 0 || pType > 7) {
					list.add(createToken(TOKEN_CHUNK_HEADER, buf.position() - 4, 4, notice("p.type.error", pType), Level.WARNING));
				} else {
					list.add(createToken(TOKEN_CHUNK_HEADER, buf.position() - 4, 4, notice("p.type." + pType), Level.INFO));
				}
				if (ptrsize == 8) {
					/* int pFlags = */ buf.getInt();
					list.add(createToken(TOKEN_METADATA, buf.position() - 4, 4));
				}
				long offset = getAddress(buf);
				list.add(createToken(TOKEN_LENGTH, buf.position() - ptrsize, ptrsize, notice("p.offset", offset), Level.INFO));
				long virtualAddr = getAddress(buf);
				list.add(createToken(TOKEN_LENGTH, buf.position() - ptrsize, ptrsize, notice("p.vaddr", virtualAddr), Level.INFO));
				long physAddr = getAddress(buf);
				list.add(createToken(TOKEN_LENGTH, buf.position() - ptrsize, ptrsize, notice("p.paddr", physAddr), Level.INFO));
				long fileSize = getAddress(buf);
				list.add(createToken(TOKEN_LENGTH, buf.position() - ptrsize, ptrsize, notice("p.filesz", fileSize), Level.INFO));
				long memSize = getAddress(buf);
				list.add(createToken(TOKEN_LENGTH, buf.position() - ptrsize, ptrsize, notice("p.memsz", memSize), Level.INFO));
				if (ptrsize == 4) {
					/* int pFlags = */ buf.getInt();
					list.add(createToken(TOKEN_METADATA, buf.position() - 4, 4));
				}
				long align = getAddress(buf);
				if (align == 0 || align == 1) {
					list.add(createToken(TOKEN_LENGTH, buf.position() - ptrsize, ptrsize, notice("align.none"), Level.INFO));
				} else {
					list.add(createToken(TOKEN_LENGTH, buf.position() - ptrsize, ptrsize, notice("align", align), Level.INFO));
				}
				//list.add(createToken(TOKEN_CHUNK, (int) offset, (int) fileSize, "Data for PH#" + segmentNumber, Level.INFO));
			}
			buf.position((int) shoff);
			int baseNameOffset = (int) getAddress(buf, (int) (shoff + shstrndx * shentsize + (ptrsize == 8 ? 0x18 : 0x10)));
			// sh (Section Header) segments
			for (int segmentNumber = 0; segmentNumber < shnum; segmentNumber++) {
				int nameOffset = buf.getInt();
				int nameOffsetInFile = baseNameOffset + nameOffset;
				int nameLength = 0;
				while (buf.get(nameOffsetInFile + nameLength) != 0) {
					nameLength++;
				}
				list.add(createToken(TOKEN_CHUNK_HEADER, buf.position() - 4, 4, new String(array, nameOffsetInFile, nameLength), Level.INFO));
				int sType = buf.getInt();
				if ((sType >= 0 && sType <= 0xb) || (sType >= 0xe && sType <= 0x13)) {
					list.add(createToken(TOKEN_CHUNK_HEADER, buf.position() - 4, 4, notice("s.type." + sType), Level.INFO));
				} else {
					list.add(createToken(TOKEN_CHUNK_HEADER, buf.position() - 4, 4, notice("s.type.error", sType), Level.WARNING));
				}
				long flags = getAddress(buf);
				ArrayList<String> flagsStr = new ArrayList<>(); {
					if ((flags & 0x1) != 0) flagsStr.add(notice("s.write"));
					if ((flags & 0x2) != 0) flagsStr.add(notice("s.alloc"));
					if ((flags & 0x4) != 0) flagsStr.add(notice("s.exec"));
					if ((flags & 0x10) != 0) flagsStr.add(notice("s.merge"));
					if ((flags & 0x20) != 0) flagsStr.add(notice("s.strings"));
					if ((flags & 0x40) != 0) flagsStr.add(notice("s.infolink"));
					if ((flags & 0x80) != 0) flagsStr.add(notice("s.linkorder"));
					if ((flags & 0x100) != 0) flagsStr.add(notice("s.nonconforming"));
					if ((flags & 0x200) != 0) flagsStr.add(notice("s.group"));
					if ((flags & 0x400) != 0) flagsStr.add(notice("s.tls"));
					if (flagsStr.isEmpty()) flagsStr.add(notice("s.noflag"));
				}
				list.add(createToken(TOKEN_METADATA, buf.position() - ptrsize, ptrsize, String.join("<br>", flagsStr), Level.INFO));
				long addr = getAddress(buf);
				list.add(createToken(TOKEN_LENGTH, buf.position() - ptrsize, ptrsize, notice("s.addr", addr), Level.INFO));
				long offset = getAddress(buf);
				list.add(createToken(TOKEN_LENGTH, buf.position() - ptrsize, ptrsize, notice("s.offset", offset), Level.INFO));
				long size = getAddress(buf);
				list.add(createToken(TOKEN_LENGTH, buf.position() - ptrsize, ptrsize, notice("s.size", size), Level.INFO));
				/* int sLink = */ buf.getInt();
				list.add(createToken(TOKEN_METADATA, buf.position() - 4, 4));
				/* int sInfo = */ buf.getInt();
				list.add(createToken(TOKEN_METADATA, buf.position() - 4, 4));
				long align = getAddress(buf);
				list.add(createToken(TOKEN_LENGTH, buf.position() - ptrsize, ptrsize, notice("align", align), Level.INFO));
				long entsize = getAddress(buf);
				list.add(createToken(TOKEN_LENGTH, buf.position() - ptrsize, ptrsize, notice("s.entsize", entsize), Level.INFO));
			}
		} catch (IndexOutOfBoundsException | BufferUnderflowException e) {
			list.add(createToken(TOKEN_NONE, array.length - 1, 1, unexpectedEOFNotice(), Level.ERROR));
		}
		return list;
	}

	private long getAddress(ByteBuffer buf) {
		return ptrsize == 8 ? buf.getLong() : buf.getInt();
	}

	private long getAddress(ByteBuffer buf, int pos) {
		return ptrsize == 8 ? buf.getLong(pos) : buf.getInt(pos);
	}

	@Override
	public String getName() {
		return "elf";
	}

	@Override
	public String[] getFileExtensions() {
		return new String[] { "elf", "o", "so", "out", "bin", "axf", "prx", "puff", "ko", "mod" };
	}

	@Override
	public byte[] getSignature() {
		return new byte[] { 0x7f, 'E', 'L', 'F' };
	}

	@Override
	public ByteOrder getEndianness() {
		return endianness;
	}
}
