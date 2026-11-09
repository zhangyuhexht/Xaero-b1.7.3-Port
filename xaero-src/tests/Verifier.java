import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Verifier {
   public static void main(String[] args) throws Exception {
      File game = new File(args[0]);
      File out = new File(args[1]);
      List<File> jars = new ArrayList();
      for(int i = 2; i < args.length; ++i) {
         jars.add(new File(args[i]));
      }

      HashSet<String> provided = new HashSet();
      provided.add(game.getPath());
      for(File j : jars) {
         provided.add(j.getPath());
      }

      HashSet<String> haveClasses = new HashSet();
      HashSet<String> haveJars = new HashSet();
      haveJars.add(game.getPath());
      collectJar(game, haveClasses);
      for(File j : jars) {
         haveJars.add(j.getPath());
         collectJar(j, haveClasses);
      }

      File[] classFiles = out.listFiles();
      int total = 0;
      int missing = 0;
      if (classFiles == null) {
         System.out.println("no classes in " + out);
         System.exit(1);
      }

      for(File cf : classFiles) {
         if (!cf.getName().endsWith(".class")) {
            continue;
         }

         String self = cf.getName().substring(0, cf.getName().length() - 6);
         haveClasses.add(self);
      }

      for(File cf : classFiles) {
         if (!cf.getName().endsWith(".class")) {
            continue;
         }

         List<String> refs = readRefs(cf);
         for(String r : refs) {
            ++total;
            if (r.startsWith("java/") || r.startsWith("javax/") || r.startsWith("sun/") || r.startsWith("[")) {
               continue;
            }

            if (!haveClasses.contains(r)) {
               System.out.println("[MISSING] " + cf.getName() + " -> " + r);
               ++missing;
            }
         }
      }

      System.out.println("scanned refs: " + total + ", missing: " + missing);
      if (missing == 0) {
         System.out.println("ALL REFERENCES OK");
      } else {
         System.exit(2);
      }
   }

   private static void collectJar(File jar, HashSet<String> into) throws Exception {
      if (!jar.isFile()) {
         return;
      }

      ZipFile zf = new ZipFile(jar);

      try {
         Enumeration<? extends ZipEntry> en = zf.entries();

         while(en.hasMoreElements()) {
            ZipEntry e = (ZipEntry)en.nextElement();
            String n = e.getName();
            if (n.endsWith(".class")) {
               into.add(n.substring(0, n.length() - 6));
            }
         }
      } finally {
         zf.close();
      }
   }

   private static List<String> readRefs(File cf) throws Exception {
      ArrayList<String> refs = new ArrayList();
      DataInputStream in = new DataInputStream(new FileInputStream(cf));

      try {
         if (in.readInt() != -889275714) {
            throw new Exception("bad magic " + cf);
         }

         in.readUnsignedShort();
         in.readUnsignedShort();
         int count = in.readUnsignedShort();
         String[] utf = new String[count];
         int[] classNameIdx = new int[count];

         for(int i = 1; i < count; ++i) {
            int tag = in.readUnsignedByte();
            switch(tag) {
            case 1:
               utf[i] = in.readUTF();
               break;
            case 3:
            case 4:
               in.readInt();
               break;
            case 5:
            case 6:
               in.readLong();
               ++i;
               break;
            case 7:
               classNameIdx[i] = in.readUnsignedShort();
               break;
            case 8:
            case 16:
               in.readUnsignedShort();
               break;
            case 9:
            case 10:
            case 11:
               in.readUnsignedShort();
               in.readUnsignedShort();
               break;
            case 12:
               in.readUnsignedShort();
               in.readUnsignedShort();
               break;
            case 15:
               in.readUnsignedByte();
               in.readUnsignedShort();
               break;
            case 18:
               in.readUnsignedShort();
               in.readUnsignedShort();
               break;
            default:
               throw new Exception("bad tag " + tag + " in " + cf);
            }
         }

         for(int i = 1; i < count; ++i) {
            if (classNameIdx[i] != 0) {
               String n = utf[classNameIdx[i]];
               if (n != null && n.length() > 0 && n.charAt(0) != '[') {
                  refs.add(n);
               }
            }
         }
      } finally {
         in.close();
      }

      return refs;
   }
}
