
require File.dirname(__FILE__) + '/spec_helper.rb'

describe "File extensions for VFS" do

  TEST_COPY_BASE = File.join( File.dirname( TEST_DATA_BASE ), 'test-copy' )

  before(:each) do
    test_copy_dir = File.join( File.dirname( TEST_DATA_DIR ), 'test-copy' )
    FileUtils.rm_rf( test_copy_dir )
    FileUtils.cp_r( TEST_DATA_DIR, test_copy_dir )
    @executor = java.util.concurrent::Executors.newScheduledThreadPool( 1 )
    @temp_file_provider = org.jboss.vfs::TempFileProvider.create( "vfs-test", @executor )

    #@archive1_path = File.join( TEST_DATA_DIR, "/home/larry/archive1.jar" )
    @archive1_path = File.join( test_copy_dir, "/home/larry/archive1.jar" )
    @archive1_file = org.jboss.vfs::VFS.child( @archive1_path )
    @archive1_mount_point = org.jboss.vfs::VFS.child( @archive1_path )
    @archive1_handle = org.jboss.vfs::VFS.mountZip( @archive1_file, @archive1_mount_point, @temp_file_provider )

    @archive2_path = "#{@archive1_path}/lib/archive2.jar"
    @archive2_file = org.jboss.vfs::VFS.child( @archive2_path )
    @archive2_mount_point = org.jboss.vfs::VFS.child( @archive2_path )
    @archive2_handle = org.jboss.vfs::VFS.mountZip( @archive2_file, @archive2_mount_point, @temp_file_provider )
  end

  after(:each) do
    @archive2_handle.close
    @archive1_handle.close
  end


  it "should report writable-ness for VFS urls" do
    prefix = File.expand_path( File.join( File.dirname( __FILE__ ), '..', TEST_COPY_BASE ) )
    url = "vfs:#{prefix}/home/larry/file1.txt" 
    File.exists?( url ).should be_true
    File.exist?( url ).should be_true
    File.writable?( url ).should be_true
  end

  it "should expand paths relative to VFS urls as absolute" do
    absolute = File.expand_path("db/development.sqlite3", "vfs:/path/to/app")
    absolute.should eql("/path/to/app/db/development.sqlite3")
  end

  it "should expand paths relative to VFS pathnames as absolute" do
    require 'pathname'
    absolute = File.expand_path("db/development.sqlite3", Pathname.new("vfs:/path/to/app"))
    absolute.should eql("/path/to/app/db/development.sqlite3")
  end

  it "should handle vfs urls as readable" do
    File.readable?( __FILE__ ).should be_true
    File.readable?( "vfs:#{__FILE__}" ).should be_true
  end
    
  [ :absolute, :relative, :vfs ].each do |style|
    describe "with #{style} paths" do
      case ( style )
        when :relative
          prefix = "./#{TEST_COPY_BASE}"
        when :absolute
          prefix = File.expand_path( File.join( File.dirname( __FILE__ ), '..', TEST_COPY_BASE ) )
        when :vfs
          prefix = "vfs:#{File.expand_path( File.join( File.dirname( __FILE__ ), '..', TEST_COPY_BASE ) )}"
      end

      it "should provide mtime for normal files" do
        mtime = File.mtime( "#{prefix}/home/larry/file1.txt" )
        mtime.should_not be_nil
      end

      it "should report writeable-ness for normal files" do
        File.writable?( "#{prefix}/home/larry/file1.txt" ).should be_true
      end

       # move to kernel_spec
      it "should allow writing with truncation via open()" do
        open( "#{prefix}/home/larry/file1.txt", (File::WRONLY | File::TRUNC | File::CREAT) ) do |file|
          file.puts "howdy"
        end
        contents = File.read( "#{prefix}/home/larry/file1.txt" )
        contents.should eql( "howdy\n" )
      end

       # move to kernel_spec
      it "should allow writing with appending via open()" do
        open( "#{prefix}/home/larry/file1.txt", (File::WRONLY | File::APPEND | File::CREAT) ) do |file|
          file.puts "howdy"
        end
        contents = File.read( "#{prefix}/home/larry/file1.txt" )
        contents.should eql( "This is file 1\nhowdy\n" )

        fs_file = File.join( File.dirname(__FILE__), '..', TEST_COPY_BASE, 'home/larry/file1.txt' )
        puts "confirm from #{fs_file}"
        fs_contents = File.read( fs_file )
        fs_contents.should eql( "This is file 1\nhowdy\n" )
      end

      it "should allow stat for normal files" do
        file = "#{prefix}/home/larry/file1.txt" 
        stat = File.stat( file )
        stat.should_not be_nil
        stat.mtime.should eql( File.mtime( file ) )
      end

      it "should provide mtime for files in an archive" do
        mtime = File.mtime( "#{prefix}/home/larry/archive1.jar/web.xml" )
        mtime.should_not be_nil
      end

      it "should allow stat for files in an archive" do
        file = "#{prefix}/home/larry/archive1.jar/web.xml"
        stat = File.stat( file )
        stat.should_not be_nil
        stat.mtime.should eql( File.mtime( file ) )
      end
    
      it "should provide mtime for files in a nested archive" do
        mtime = File.mtime( "#{prefix}/home/larry/archive1.jar/lib/archive2.jar/manifest.txt" )
        mtime.should_not be_nil
      end
    
      it "should test existance of normal files" do
        File.exist?( "#{prefix}/home/larry/file1.txt" ).should be_true
        File.exist?( "#{prefix}/home/larry/file42.txt" ).should be_false
      end
    
      it "should test existance of files in an archive" do
        File.exist?( "#{prefix}/home/larry/archive1.jar/web.xml" ).should be_true
      end
    
      it "should test directoryness for normal files" do
        File.directory?( "#{prefix}/home/larry" ).should be_true
        File.directory?( "#{prefix}/home/larry/file1.txt" ).should be_false
      end

      it "should test directoryness for files within an archive" do
        File.directory?( "#{prefix}/home/larry/archive1.jar/lib" ).should be_true
        File.directory?( "#{prefix}/home/larry/archive1.jar/web.xml" ).should be_false
      end

      it "should test directoryness for non-existant files" do
        File.directory?( "#{prefix}/home/larry/archive1.jar/fib" ).should be_false
        File.directory?( "#{prefix}/home/larry/archive1.jar/tacos" ).should be_false
        File.directory?( "#{prefix}/tacos" ).should be_false
      end

      it "should test fileness for normal files" do
        File.file?( "#{prefix}/home/larry" ).should be_false
        File.file?( "#{prefix}/home/larry/file1.txt" ).should be_true
      end

      it "should test fileness for files within an archive" do
        File.file?( "#{prefix}/home/larry/archive1.jar/lib" ).should be_false
        File.file?( "#{prefix}/home/larry/archive1.jar/web.xml" ).should be_true
      end
    end
  end
end
