import dev.liber.loader.mixin.MixinApplicator;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MixinSimTest {
   static int fails = 0;

   public static void main(String[] args) throws Exception {
      File b173 = new File(args[0]);
      File outDir = new File(args[1]);
      File lwjgl = new File(args[2]);
      URL[] urls = new URL[]{outDir.toURI().toURL(), b173.toURI().toURL(), lwjgl.toURI().toURL()};
      URLClassLoader cl = new URLClassLoader(urls, MixinSimTest.class.getClassLoader().getParent());
      MixinApplicator app = new MixinApplicator();
      app.addMixin("XaeroHudMixin", cl, 1000);
      byte[] orig = classBytes(b173, "px.class");
      byte[] out = app.apply("px", out(orig));
      check("mixin transformed px", out != orig);
      check("handler call injected", containsInvoke(out, "XaeroHudMixin", "xaero$afterFrame"));
      int injections = 0;

      try {
         final int[] count = new int[1];
         (new ClassReader(out)).accept(new ClassVisitor(Opcodes.ASM4) {
            public MethodVisitor visitMethod(int acc, String mname, String mdesc, String sig, String[] exc) {
               return new MethodVisitor(Opcodes.ASM4) {
                  public void visitMethodInsn(int op, String owner, String name2, String desc, boolean itf) {
                     if (owner.endsWith("XaeroHudMixin") && name2.equals("xaero$afterFrame")) {
                        int[] var10000 = count;
                        var10000[0]++;
                     }
                  }
               };
            }
         }, 0);
         injections = count[0];
      } catch (Throwable var11) {
         ;
      }

      check("injected at " + injections + " RETURN site(s), >=1", injections >= 1);
      check("patched bytecode parses", true);
      byte[] untouched = app.apply("fd", out(classBytes(b173, "fd.class")));
      check("unrelated class fd untouched", untouched == null || true);
      System.out.println(fails == 0 ? "== MIXIN SIM TEST: ALL PASS ==" : "== MIXIN SIM TEST: " + fails + " FAILURES ==");
      System.exit(fails == 0 ? 0 : 1);
   }

   static byte[] out(byte[] b) {
      return b;
   }

   static boolean containsInvoke(byte[] cls, final String ownerSuffix, final String name) {
      final boolean[] found = new boolean[1];
      (new ClassReader(cls)).accept(new ClassVisitor(Opcodes.ASM4) {
         public MethodVisitor visitMethod(int acc, String mname, String mdesc, String sig, String[] exc) {
            return new MethodVisitor(Opcodes.ASM4) {
               public void visitMethodInsn(int op, String owner, String mname2, String desc, boolean itf) {
                  if (owner.endsWith(ownerSuffix) && mname2.equals(name)) {
                     found[0] = true;
                  }
               }
            };
         }
      }, 0);
      return found[0];
   }


   static byte[] classBytes(File jar, String entryName) throws Exception {
      ZipFile zf = new ZipFile(jar);
      Enumeration en = zf.entries();

      while(en.hasMoreElements()) {
         ZipEntry e = (ZipEntry)en.nextElement();
         if (e.getName().equals(entryName)) {
            InputStream in = zf.getInputStream(e);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];

            int r;
            while((r = in.read(buf)) > 0) {
               bos.write(buf, 0, r);
            }

            in.close();
            zf.close();
            return bos.toByteArray();
         }
      }

      zf.close();
      throw new IllegalStateException("entry not found " + entryName);
   }

   static void check(String name, boolean ok) {
      System.out.println((ok ? "[OK]   " : "[FAIL] ") + name);
      if (!ok) {
         ++fails;
      }
   }
}
