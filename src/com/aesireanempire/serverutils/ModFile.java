package com.aesireanempire.serverutils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class ModFile
{

    private String fileName = "";
    private String modID = "";

    public ModFile(ZipFile zip, ZipEntry element) throws IOException
    {
        this.fileName = element.getName();

        if (this.fileName.contains(".jar") || this.fileName.contains(".zip"))
        {
            getMCModInfoBuffer(zip.getInputStream(element));

            if (this.modID.isEmpty())
            {
                getModIdFallBack(this.fileName.substring(this.fileName.lastIndexOf("/") + 1));
            }
        }
    }

    private void getModIdFallBack(String fileName)
    {
        for (int i = 0; i < fileName.length() - 1; i++)
        {
            if (fileName.substring(i, i + 1).matches("\\d"))
            {
                if (fileName.substring(i - 1, i).matches("\\W"))
                {
                    String[] split = fileName.split(fileName.substring(i - 1, i));

                    if (split.length > 0)
                    {
                        this.modID = split[0];
                        break;
                    }
                }
                else
                {
                    this.modID = fileName.substring(0, i);
                    break;
                }
            }
        }
    }

    private void getMCModInfoBuffer(InputStream inputStream) throws IOException
    {
        ZipInputStream stream = new ZipInputStream(inputStream);

        ZipEntry entry;
        while ((entry = stream.getNextEntry()) != null)
        {
            if (entry.getName().equals("mcmod.info"))
            {
                readMCModInfo(stream);
            }
        }
        stream.close();
    }

    private void readMCModInfo(InputStream inputStream)
    {
        Scanner scanner = new Scanner(inputStream);

        while (scanner.hasNextLine())
        {
            String line = scanner.nextLine();
            line = line.trim().replace(" ", "").replace(",", "").replace("\"", "");
            if (line.toLowerCase().startsWith("modid"))
            {
                String[] split = line.split(":");

                if (this.modID.isEmpty() && split.length == 2)
                {
                    this.modID = split[1];
                }
            }
        }
    }

    @Override
    public String toString()
    {
        return this.modID.isEmpty() ? this.fileName : this.modID;
    }

    public String getFileName()
    {
        return this.fileName;
    }

    public String getModID()
    {
        return this.modID;
    }
}
