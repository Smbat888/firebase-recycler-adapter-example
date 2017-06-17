package vtc.room.a101.firebaserecyclerexample;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import vtc.room.a101.firebaserecyclerexample.models.GalleryItem;

import static vtc.room.a101.firebaserecyclerexample.constants.Constants.CHOOSER_TITLE;
import static vtc.room.a101.firebaserecyclerexample.constants.Constants.GALLERY_REF;
import static vtc.room.a101.firebaserecyclerexample.constants.Constants.REQUEST_PIC_GALLERY;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private DatabaseReference mDatabaseRef;
    private StorageReference mStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        init();
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, CHOOSER_TITLE),
                        REQUEST_PIC_GALLERY);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_PIC_GALLERY && resultCode == RESULT_OK ){
            final Uri uri = data.getData();
            final StorageReference filePath = mStorage.child(uri.getLastPathSegment());
            //TODO show progress dialog to upload image and hide after showing in recycler view
            filePath.putFile(uri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    Toast.makeText(MainActivity.this, "Uploaded", Toast.LENGTH_LONG).show();
                    //TODO change title pass way
                    addNewItem(Uri.parse(filePath.toString()), "JustTitle");
                }
            });
        }
    }

    /**
     *  Initializes the screen and necessary components
     */
    private void init() {
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 2)); //2 --> grid columns count
        mDatabaseRef= FirebaseDatabase.getInstance().getReference().child(GALLERY_REF);
        mStorage = FirebaseStorage.getInstance().getReference();
        setupFirebaseAdapter();
    }

    /**
     * Set up firebase recycler adapter and populates view item
     */
    private void setupFirebaseAdapter() {
        FirebaseRecyclerAdapter<GalleryItem, GalleryItemViewHolder> firebaseAdapter =
                new FirebaseRecyclerAdapter<GalleryItem, GalleryItemViewHolder>(GalleryItem.class,
                        R.layout.gallery_item,
                        GalleryItemViewHolder.class,
                        mDatabaseRef) {
                    @Override
                    public void populateViewHolder(GalleryItemViewHolder galleryItemViewHolder,
                            GalleryItem galleryItem, int position) {
                        if (galleryItem.getImage() != null && galleryItem.getTitle() != null) {
                            galleryItemViewHolder.setImage(galleryItem.getImage(),
                                    MainActivity.this);
                            galleryItemViewHolder.setTitle(galleryItem.getTitle());
                        }

                    }
                };
        mRecyclerView.setAdapter(firebaseAdapter);
    }

    /**
     * Adds new item in database and also corresponding in recycler view
     *
     * @param fileLocation file location from firebase storage
     * @param title item title to write in database and show in recycler card
     */
    private void addNewItem(Uri fileLocation, String title){
        final GalleryItem galleryItem = new GalleryItem();
        galleryItem.setImage(fileLocation.toString());
        galleryItem.setTitle(title);
        mDatabaseRef.child(getSaltString()).setValue(galleryItem);
    }

    /**
     * Gets random alpha-numeric string as a key in database, specific for each item
     *
     * @return random string
     */
    private String getSaltString() {
        final String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 18) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        return salt.toString();
    }

    public static class GalleryItemViewHolder extends RecyclerView.ViewHolder{
        private TextView title;
        private ImageView image;

        public GalleryItemViewHolder(View itemView) {
            super(itemView);

            title = (TextView) itemView.findViewById(R.id.card_title);
            image = (ImageView) itemView.findViewById(R.id.card_image);
        }

        void setImage(final String imagePath, final Context context) {
            final StorageReference myStorage = FirebaseStorage.getInstance()
                    .getReferenceFromUrl(imagePath);
            Glide.with(context)
                    .using(new FirebaseImageLoader())
                    .load(myStorage)
                    .into(image);
        }

        void setTitle(final String titleStr) {
            title.setText(titleStr);
        }

    }

}
